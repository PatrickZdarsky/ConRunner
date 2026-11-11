import { effect, Injectable, Signal, inject } from "@angular/core";
import {KEYCLOAK_EVENT_SIGNAL, KeycloakEvent, KeycloakEventType, ReadyArgs, typeEventArgs} from "keycloak-angular";
import Keycloak, {KeycloakProfile} from "keycloak-js";
import {BehaviorSubject, map, Observable, tap} from "rxjs";
import {UserProfileDto, UserProfileEndpointService} from "../../../shiftservice-client";

@Injectable({
  providedIn: "root"
})
export class UserService {
  private readonly keycloak = inject(Keycloak);
  private readonly userService = inject(UserProfileEndpointService);

  private _kcProfile$ = new BehaviorSubject<KeycloakProfile | null>(null);
  private _userProfile$ = new BehaviorSubject<UserProfileDto | null>(null);
  private _isPlatformAdmin$ = new BehaviorSubject<boolean>(false);

  constructor() {
    const eventSignal = inject<Signal<KeycloakEvent>>(KEYCLOAK_EVENT_SIGNAL);

    // React to every Keycloak events
    effect(() => {
      const event = eventSignal();

      if (
        event.type === KeycloakEventType.Ready
        || event.type === KeycloakEventType.AuthSuccess
        || event.type === KeycloakEventType.AuthRefreshSuccess
        || event.type === KeycloakEventType.AuthLogout
        || event.type === KeycloakEventType.AuthError
        || event.type === KeycloakEventType.AuthRefreshError
      ) {
        void this.syncUserState(event);
      }
    });

    this._userProfile$.pipe(
      map(profile => profile?.account.platformAdmin === true)
    ).subscribe(isPlatformAdmin => this._isPlatformAdmin$.next(isPlatformAdmin));
  }

  private async syncUserState(event: KeycloakEvent) {
    const isReadyEvent = event.type === KeycloakEventType.Ready;
    const isAuthenticated = isReadyEvent
      ? typeEventArgs<ReadyArgs>(event.args)
      : this.keycloak.authenticated === true;

    if (!isAuthenticated) {
      this._kcProfile$.next(null);
      this._userProfile$.next(null);
      return;
    }

    try {
      const profile = await this.keycloak.loadUserProfile();
      this._kcProfile$.next(profile);
    } catch {
      this._kcProfile$.next(null);
    }

    this.userService.getCurrentUserProfile().pipe(
      tap(user => this._userProfile$.next(user))
    ).subscribe({
      error: () => {
        if (this.keycloak.authenticated !== true) {
          this._userProfile$.next(null);
        }
      }
    });
  }

  /**
   * Observable of the currently logged-in profile
   * Changes when logged in/out
   */
  public get kcProfile$(): Observable<KeycloakProfile | null> {
    return this._kcProfile$.asObservable();
  }

  public get userProfile$() {
    return this._userProfile$.asObservable();
  }

  public get isPlatformAdmin$() {
    return this._isPlatformAdmin$.asObservable();
  }

  /**
   * Get the account management URL via keycloak
   */
  public get manageUrl() {
    const baseUrl = this.keycloak.createAccountUrl();
    return `${baseUrl}?referrer=${window.location.origin}&referrer_uri=${window.location.origin}`;
  }

  public get token() {
    return this.keycloak.token;
  }

  public get isAuthenticated() {
    return this.keycloak.authenticated === true;
  }

  public async ensureValidToken(minValidity = 30): Promise<string | undefined> {
    if (!this.isAuthenticated) {
      return undefined;
    }

    try {
      await this.keycloak.updateToken(minValidity);
    } catch {
      return undefined;
    }

    return this.keycloak.token;
  }

  /**
   * Check if the current user can manage the given plan
   * admins have access to all plans
   * planner to those assigned
   * @param planId
   */
  public canManagePlan$(planId: string){
    return this.userProfile$.pipe(
      map(profile => {
        if(profile === null) {return false;}

        if (profile.account.platformAdmin === true) {
          return true;
        }

        return profile.planningPlans.some(allowedPlanId => allowedPlanId === planId);
      })
    );
  }

  /**
   * Check if the current user can participate in the given plan
   * admins have access to all plans
   * planners and volunteers to those assigned
   * planner is treated as volunteer
   * @param planId
   */
  public canParticipatePlan$(planId: string){
    return this.userProfile$.pipe(
      map(profile => {
        if(profile === null) {return false;}

        return profile.volunteeringPlans.some(allowedPlanId => allowedPlanId === planId)
          || profile.planningPlans.some(allowedPlanId => allowedPlanId === planId);
      })
    );
  }

  /**
   * Initialize keycloak login process
   */
  public login(redirectTo?: string) {
    this.keycloak.login({redirectUri: redirectTo ?? window.location.origin});
  }

  public refreshProfile() {
    return this.userService.getCurrentUserProfile().pipe(
      tap((user) => this._userProfile$.next(user))
    );
  }

  /**
   * Initialize keycloak logout process
   */
  public logout() {
    this.keycloak.logout({redirectUri: window.location.origin});
  }
}
