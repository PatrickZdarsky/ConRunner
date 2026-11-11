import {CanActivateFn, Router} from "@angular/router";
import {inject} from "@angular/core";
import {UserService} from "../../services/user/user.service";
import {filter, map} from "rxjs";
import {ToastService} from "../../services/toast/toast.service";

export const isAdminGuard: CanActivateFn = () => {
    const userService = inject(UserService);
    const toastService = inject(ToastService);
    const router = inject(Router);

    return userService.userProfile$.pipe(
      filter(profile => profile !== null),
      map(user => user !== null && user.account.platformAdmin === true),
      map(success => {
        if(!success) {
          toastService.showError("Access Denied", "Only Administrators may view this page");
          return router.createUrlTree(["/"]);
        }

        return success;
      })
    );
  };
