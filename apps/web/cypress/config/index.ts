export type AppConfig = {
  BASE_URL: string;
  KEYCLOAK_URL: string;
  TIMEOUT_XS: number;
  TIMEOUT_S: number;
  TIMEOUT_M: number;
  TIMEOUT_L: number;
  MIN_DOM: number;
  API_VERSION: string;
  KEYCLOAK_REALM: string;
};

export const APP_CONFIG: AppConfig = {
  BASE_URL: loadStringValue('BASE_URL'),
  KEYCLOAK_URL: loadStringValue('KEYCLOAK_URL'),
  TIMEOUT_XS: loadNumberValue('TIMEOUT_XS'),
  TIMEOUT_S: loadNumberValue('TIMEOUT_S'),
  TIMEOUT_M: loadNumberValue('TIMEOUT_M'),
  TIMEOUT_L: loadNumberValue('TIMEOUT_L'),
  API_VERSION: loadStringValue('API_VERSION'),
  MIN_DOM: loadNumberValue('MIN_DOM'),
  KEYCLOAK_REALM: loadStringValue('KEYCLOAK_REALM'),
};

function loadStringValue(key: keyof AppConfig): string {
  return String(Cypress.expose(key) ?? '');
}

function loadNumberValue(key: keyof AppConfig): number {
  return Number.parseInt(loadStringValue(key), 10) || 0;
}
