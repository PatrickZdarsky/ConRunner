#!/usr/bin/env bash
set -e

APP_DIR="/usr/share/nginx/html"
ENV_FILE="$APP_DIR/env.js"

echo "Injecting runtime environment variables into Angular app..."

# Replace placeholders in the dedicated runtime config file only, so hashed bundles stay immutable.
sed -i \
  -e "s|KEYCLOAK_URL_PLACEHOLDER|${KEYCLOAK_URL}|g" \
  -e "s|KEYCLOAK_REALM_PLACEHOLDER|${KEYCLOAK_REALM}|g" \
  -e "s|KEYCLOAK_CLIENT_ID_PLACEHOLDER|${KEYCLOAK_CLIENT_ID}|g" \
  -e "s|SHIFTSERVICE_BASE_PATH_PLACEHOLDER|${SHIFTSERVICE_BASE_PATH}|g" \
  -e "s|AUDITSERVICE_BASE_PATH_PLACEHOLDER|${AUDITSERVICE_BASE_PATH}|g" \
  -e "s|NOTIFICATIONSERVICE_BASE_PATH_PLACEHOLDER|${NOTIFICATIONSERVICE_BASE_PATH}|g" \
  "$ENV_FILE"

echo "Starting nginx..."

exec "$@"
