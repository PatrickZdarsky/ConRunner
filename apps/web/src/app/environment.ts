interface RuntimeEnvironment {
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
  apiBasePath: string;
}

type WindowWithRuntimeEnvironment = Window & {
  __SHIFTCONTROL_ENV__?: Partial<RuntimeEnvironment>;
};

const runtimeEnvironment = (window as WindowWithRuntimeEnvironment).__SHIFTCONTROL_ENV__ ?? {};

function readRuntimeEnvironmentValue(
  key: keyof RuntimeEnvironment,
  fallbackValue: string
): string {
  const value = runtimeEnvironment[key];

  if (typeof value === "string" && value.length > 0 && !value.includes("_PLACEHOLDER")) {return value;}
  if (window.location.hostname === "localhost") {
    const local: Record<keyof RuntimeEnvironment, string> = {
      keycloakUrl: "http://localhost:8180", keycloakRealm: "shiftcontrol", keycloakClientId: "shiftcontrol-local",
      apiBasePath: window.location.origin
    };
    return local[key];
  }
  return fallbackValue;
}

export const environment: RuntimeEnvironment = {
  keycloakUrl: readRuntimeEnvironmentValue("keycloakUrl", "KEYCLOAK_URL_PLACEHOLDER"),
  keycloakRealm: readRuntimeEnvironmentValue("keycloakRealm", "KEYCLOAK_REALM_PLACEHOLDER"),
  keycloakClientId: readRuntimeEnvironmentValue("keycloakClientId", "KEYCLOAK_CLIENT_ID_PLACEHOLDER"),
  apiBasePath: readRuntimeEnvironmentValue("apiBasePath", "API_BASE_PATH_PLACEHOLDER")
};
