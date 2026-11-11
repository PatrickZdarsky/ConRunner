#!/usr/bin/env bash
set -Eeuo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
web="$root/apps/web"
tmp=$(mktemp -d)
container=''
backend_pid=''
cleanup() {
  if [[ -n "$backend_pid" ]]; then kill -- "-$backend_pid" 2>/dev/null || true; wait "$backend_pid" 2>/dev/null || true; fi
  if [[ -n "$container" ]]; then docker rm -f "$container" >/dev/null 2>&1 || true; fi
  rm -rf "$tmp"
}
trap cleanup EXIT INT TERM
command -v docker >/dev/null || { echo 'Docker is required for isolated API generation.' >&2; exit 1; }
if [[ ! -x "$web/node_modules/.bin/openapi-generator-cli" ]]; then
  echo 'Installing frontend dependencies...'
  (cd "$web" && CYPRESS_INSTALL_BINARY=0 npm ci --no-audit --no-fund)
fi
container=$(docker run -d --rm -e POSTGRES_DB=shiftcontrol -e POSTGRES_USER=shiftcontrol -e POSTGRES_PASSWORD=shiftcontrol -p 127.0.0.1::5432 -v "$root/ops/compose/postgres/01-clob.sql:/docker-entrypoint-initdb.d/01-clob.sql:ro" postgres:16-alpine)
for _ in {1..60}; do
  if docker exec "$container" pg_isready -U shiftcontrol -d shiftcontrol >/dev/null 2>&1; then break; fi
  sleep 1
done
docker exec "$container" pg_isready -U shiftcontrol -d shiftcontrol >/dev/null || { echo 'Temporary database did not start.' >&2; exit 1; }
db_port=$(docker port "$container" 5432/tcp | head -1 | sed 's/.*://')
api_port=$(python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()')
echo 'Starting an isolated backend to generate the API specification...'
# Compile a snapshot separately so generation cannot disrupt a running dev backend.
mkdir -p "$tmp/repo/apps/backend"
cp "$root/pom.xml" "$root/mvnw" "$tmp/repo/"
cp -a "$root/.mvn" "$tmp/repo/"
cp "$root/apps/backend/pom.xml" "$tmp/repo/apps/backend/"
cp -a "$root/apps/backend/src" "$tmp/repo/apps/backend/"
(cd "$tmp/repo" && exec setsid ./mvnw -q -pl apps/backend spring-boot:run -Dspring-boot.run.profiles=local "-Dspring-boot.run.arguments=--server.port=$api_port --spring.datasource.url=jdbc:postgresql://localhost:$db_port/shiftcontrol --spring.devtools.restart.enabled=false") >"$tmp/backend.log" 2>&1 &
backend_pid=$!
ready=false
for _ in {1..180}; do
  if curl --connect-timeout 1 -fsS "http://127.0.0.1:$api_port/v3/api-docs" -o "$tmp/openapi.json" 2>/dev/null; then ready=true; break; fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then break; fi
  sleep 1
done
if [[ "$ready" != true ]]; then echo 'Backend failed to generate OpenAPI:' >&2; tail -80 "$tmp/backend.log" >&2; exit 1; fi
python3 - "$tmp/openapi.json" <<'PY'
import json, sys
with open(sys.argv[1]) as f: spec = json.load(f)
if not spec.get('openapi') or not spec.get('paths'):
    raise SystemExit('Backend returned an invalid or empty OpenAPI document')
for path, method in [('/api/v1/me/notification-preferences', 'get'),
                     ('/api/v1/me/notifications', 'get'),
                     ('/api/v1/realtime/ticket', 'post'),
                     ('/api/v1/audit', 'get')]:
    responses = spec['paths'][path][method]['responses']
    if not any('application/json' in response.get('content', {}) for code, response in responses.items() if code.startswith('2')):
        raise SystemExit(f'{method.upper()} {path} does not describe a JSON response')
spec['servers'] = [{'url': '/'}]
with open(sys.argv[1], 'w') as f: json.dump(spec, f, sort_keys=True, separators=(',', ':'))
PY
echo 'Generating Angular API client...'
(cd "$web" && ./node_modules/.bin/openapi-generator-cli generate -i "$tmp/openapi.json" -g typescript-angular -o "$tmp/api-client" --additional-properties=fileNaming=kebab-case,withInterfaces=true,ngVersion=19.2.0 --generate-alias-as-model --type-mappings set=Array)
mkdir -p "$root/apps/backend/target"
cp "$tmp/openapi.json" "$root/apps/backend/target/openapi.json"
rm -rf "$web/src/api-client"
mv "$tmp/api-client" "$web/src/api-client"
echo 'Generated apps/web/src/api-client from this checkout.'
