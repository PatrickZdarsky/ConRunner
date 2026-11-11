import {ApplicationConfig, inject, LOCALE_ID, provideZoneChangeDetection} from "@angular/core";
import { provideRouter } from "@angular/router";

import { routes } from "./app.routes";
import {AutoRefreshTokenService, provideKeycloak, UserActivityService, withAutoRefreshToken} from "keycloak-angular";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import {Configuration as ShiftserviceConfiguration} from "../shiftservice-client";
import {Configuration as AuditserviceConfiguration} from "../auditservice-client";
import Keycloak from "keycloak-js";
import {provideAnimations} from "@angular/platform-browser/animations";
import {provideToastr} from "ngx-toastr";
import {ToastComponent} from "./components/toast/toast.component";
import {environment} from "./environment";
import {authTokenInterceptor} from "./interceptors/auth-token.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [provideZoneChangeDetection({ eventCoalescing: true }), provideRouter(routes), provideKeycloak({
    config: {
      url: environment.keycloakUrl,
      realm: environment.keycloakRealm,
      clientId: environment.keycloakClientId
    },
    initOptions: {
      onLoad: "check-sso",
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html"
    },
    features: [
      withAutoRefreshToken({
        onInactivityTimeout: "logout",
        sessionTimeout: 60000 * 30, // 30 minutes
      })
    ],
    providers: [AutoRefreshTokenService, UserActivityService, provideAnimations(), provideToastr({
      toastComponent: ToastComponent,
      closeButton: true,
      easeTime: 100,
      timeOut: 5000,
    }), provideHttpClient(withInterceptors([authTokenInterceptor])),
      {provide: LOCALE_ID, useValue: "de-AT" },
      {
        provide: ShiftserviceConfiguration,
        useFactory: () => {
          const keycloak = inject(Keycloak);

          return new ShiftserviceConfiguration({
            basePath: environment.shiftserviceBasePath,
            credentials: {
              "bearerAuth": () => keycloak.token
            }
          });
        },
        deps: [Keycloak],
        multi: false
      },
      {
        provide: AuditserviceConfiguration,
        useFactory: () => {
          const keycloak = inject(Keycloak);

          return new AuditserviceConfiguration({
            basePath: environment.auditserviceBasePath,
            credentials: {
              "bearerAuth": () => keycloak.token
            }
          });
        },
        deps: [Keycloak],
        multi: false
      }
      ]
  }),]
};
