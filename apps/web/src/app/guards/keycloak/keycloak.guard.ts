import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from "@angular/router";
import { inject } from "@angular/core";
import { AuthGuardData, createAuthGuard } from "keycloak-angular";

const isAccessAllowed = async (
  route: ActivatedRouteSnapshot,
  _: RouterStateSnapshot,
  authData: AuthGuardData
): Promise<boolean | UrlTree> => {
  const { authenticated } = authData;

  if (authenticated) {
    return true;
  }

  const currentUrl = window.location.href;
  const currentUrlEncoded = encodeURIComponent(currentUrl);

  const router = inject(Router);
  return router.parseUrl("/login?continue=" + currentUrlEncoded);
};

const isNotLoggedIn = async (
  _: ActivatedRouteSnapshot,
  __: RouterStateSnapshot,
  authData: AuthGuardData
): Promise<boolean | UrlTree> => {
  const { authenticated } = authData;

  if (!authenticated) {
    return true;
  }

  const router = inject(Router);
  return router.parseUrl("/");
};

export const authenticatedGuard = createAuthGuard<CanActivateFn>(isAccessAllowed);
export const notLoggedInGuard = createAuthGuard<CanActivateFn>(isNotLoggedIn);
