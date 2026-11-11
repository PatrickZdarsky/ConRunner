import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { from } from "rxjs";
import { switchMap } from "rxjs/operators";
import { environment } from "../environment";
import { UserService } from "../services/user/user.service";

function isProtectedApiRequest(url: string): boolean {
  const requestUrl = new URL(url, window.location.origin);
  const apiBase = new URL(environment.apiBasePath, window.location.origin);
  return requestUrl.origin === apiBase.origin && requestUrl.pathname.startsWith("/api/");
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
