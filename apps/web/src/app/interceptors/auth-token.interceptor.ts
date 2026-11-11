import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { from } from "rxjs";
import { switchMap } from "rxjs/operators";
import { environment } from "../environment";
import { UserService } from "../services/user/user.service";

function normalizeUrl(url: string): string {
  return new URL(url, window.location.origin).href.replace(/\/$/, "");
}

function isProtectedApiRequest(url: string): boolean {
  const requestUrl = normalizeUrl(url);
  const protectedBasePaths = [
    environment.shiftserviceBasePath,
    environment.auditserviceBasePath,
  ]
    .filter(basePath => !basePath.includes("PLACEHOLDER"))
    .map(normalizeUrl);

  return protectedBasePaths.some(basePath => requestUrl.startsWith(basePath));
}

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isProtectedApiRequest(request.url)) {
    return next(request);
  }

  const userService = inject(UserService);

  return from(userService.ensureValidToken()).pipe(
    switchMap(token => {
      const headers = token
        ? request.headers.set("Authorization", `Bearer ${token}`)
        : request.headers.delete("Authorization");

      return next(request.clone({ headers }));
    })
  );
};
