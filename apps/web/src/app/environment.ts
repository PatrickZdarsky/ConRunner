type RuntimeEnvironment = {
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
  shiftserviceBasePath: string;
  auditserviceBasePath: string;
  notificationserviceBasePath: string;
};

type WindowWithRuntimeEnvironment = Window & {
  __SHIFTCONTROL_ENV__?: Partial<RuntimeEnvironment>;
};

const runtimeEnvironment = (window as WindowWithRuntimeEnvironment).__SHIFTCONTROL_ENV__ ?? {};

function readRuntimeEnvironmentValue(
  key: keyof RuntimeEnvironment,
  fallbackValue: string
): string {
  const value = runtimeEnvironment[key];

  return typeof value === "string" && value.length > 0 ? value : fallbackValue;
}

export const environment: RuntimeEnvironment = {
  keycloakUrl: readRuntimeEnvironmentValue("keycloakUrl", "KEYCLOAK_URL_PLACEHOLDER"),
  keycloakRealm: readRuntimeEnvironmentValue("keycloakRealm", "KEYCLOAK_REALM_PLACEHOLDER"),
  keycloakClientId: readRuntimeEnvironmentValue("keycloakClientId", "KEYCLOAK_CLIENT_ID_PLACEHOLDER"),
  shiftserviceBasePath: readRuntimeEnvironmentValue("shiftserviceBasePath", "SHIFTSERVICE_BASE_PATH_PLACEHOLDER"),
  auditserviceBasePath: readRuntimeEnvironmentValue("auditserviceBasePath", "AUDITSERVICE_BASE_PATH_PLACEHOLDER"),
  notificationserviceBasePath: readRuntimeEnvironmentValue("notificationserviceBasePath", "NOTIFICATIONSERVICE_BASE_PATH_PLACEHOLDER")
};
