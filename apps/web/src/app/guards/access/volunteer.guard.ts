import {ActivatedRouteSnapshot, CanActivateFn, Router} from "@angular/router";
import {inject} from "@angular/core";
import {UserService} from "../../services/user/user.service";
import {filter, map} from "rxjs";
import {ToastService} from "../../services/toast/toast.service";

export const isVolunteerInEventGuard:
  ((planParam: string, allowAdmin?: boolean) => CanActivateFn) =
  (eventIdParam: string, allowAdmin: boolean = true) => (route: ActivatedRouteSnapshot) => {
    const router = inject(Router);
    const userService = inject(UserService);
    const toastService = inject(ToastService);

    const eventId = route.paramMap.get(eventIdParam);

    if(eventId === null) {
      console.warn("Missing event url param");
      return router.createUrlTree(["/"]);
    }

    return userService.userProfile$.pipe(
      filter(profile => profile !== null),
      map(user => user !== null && (
        user.volunteeringEvents.some(event => event === eventId) ||
        user.planningEvents.some(event => event === eventId) ||
        allowAdmin && user.account.platformAdmin === true)
      ),
      map(success => {
        if(!success) {
          toastService.showError("Access Denied", "You have not joined this event as a Volunteer");
          return router.createUrlTree(["/"]);
        }

        return success;
      })
    );
  };
