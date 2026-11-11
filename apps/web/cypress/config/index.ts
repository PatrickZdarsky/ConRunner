export type AppConfig = {
  BASE_URL: string;
  KEYCLOAK_URL: string;
  TIMEOUT_XS: number;
  TIMEOUT_S: number;
  TIMEOUT_M: number;
  TIMEOUT_L: number;
  MIN_DOM: number;
  USERNAME: string;
  PASSWORD: string;
  API_VERSION: string;
  API_KEY: string;
  KEYCLOAK_REALM: string;
};

type EnvKeys =
  | 'BASE_URL'
  | 'KEYCLOAK_URL'
  | 'TIMEOUT_XS'
  | 'TIMEOUT_S'
  | 'TIMEOUT_M'
  | 'TIMEOUT_L'
  | 'USERNAME'
  | 'PASSWORD'
  | 'API_VERSION'
  | 'API_KEY'
  | 'KEYCLOAK_REALM'
  | 'MIN_DOM'

type StageName = 'local';

type StageCfg = {
  BASE_URL: string;
  KEYCLOAK_URL: string;
  MAILHOG: string;
  USERNAME: string;
  PASSWORD: string;
  API_KEY: string;
  KEYCLOAK_REALM: string;
};

function getStageCfg(): StageCfg | undefined {
  const stages = Cypress.env('STAGES') as Record<string, StageCfg>;
  const stageEnv = Cypress.env('STAGE') as string;
  const stage = stageEnv as StageName
  return stages ? stages[stage] : undefined;
}

const activeStageCfg = getStageCfg();

export const APP_CONFIG: AppConfig = {
  BASE_URL: activeStageCfg ? activeStageCfg.BASE_URL : loadStringValue('BASE_URL'),
  KEYCLOAK_URL: activeStageCfg ? activeStageCfg.KEYCLOAK_URL : loadStringValue('KEYCLOAK_URL'),
  TIMEOUT_XS: loadNumberValue('TIMEOUT_XS'),
  TIMEOUT_S: loadNumberValue('TIMEOUT_S'),
  TIMEOUT_M: loadNumberValue('TIMEOUT_M'),
  TIMEOUT_L: loadNumberValue('TIMEOUT_L'),
  API_VERSION: loadStringValue('API_VERSION'),
  MIN_DOM: loadNumberValue('MIN_DOM'),
  USERNAME: activeStageCfg ? activeStageCfg.USERNAME : loadStringValue('USERNAME'),
  PASSWORD: activeStageCfg ? activeStageCfg.PASSWORD : loadStringValue('PASSWORD'),
  API_KEY: activeStageCfg ? activeStageCfg.API_KEY : loadStringValue('API_KEY'),
  KEYCLOAK_REALM: activeStageCfg ? activeStageCfg.KEYCLOAK_REALM : loadStringValue('KEYCLOAK_REALM'),
};

/**
 * Loads Cypress environment variables.
 * @param key
 */
function loadStringValue(key: EnvKeys): string {
  return (Cypress.env(key) as string) ?? '';
}

function loadNumberValue(key: EnvKeys): number {
  const value = loadStringValue(key);
  try {
    return parseInt(value);
  } catch {
    return 0;
  }
}
