import {Component, inject, OnDestroy} from "@angular/core";
import {PageService} from "../../../services/page/page.service";
import {BC_EVENT, BC_PLAN_ONBOARDING} from "../../../breadcrumbs";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {ShiftPlanInviteDetailsDto, ShiftPlanInviteEndpointService} from "../../../../shiftservice-client";
import {BehaviorSubject, combineLatestWith, filter, map, Subscription, switchMap, take, withLatestFrom} from "rxjs";
import {AsyncPipe, DatePipe} from "@angular/common";
import {TooltipDirective} from "../../../directives/tooltip.directive";
import {icons} from "../../../util/icons";
import {UserService} from "../../../services/user/user.service";
import {mapValue} from "../../../util/value-maps";
import {ToastService} from "../../../services/toast/toast.service";

@Component({
  selector: "app-plan-onboarding",
  imports: [
    InputButtonComponent,
    RouterLink,
    FaIconComponent,
    AsyncPipe,
    TooltipDirective,
    DatePipe
  ],
  standalone: true,
  templateUrl: "./plan-onboarding.component.html",
  styleUrl: "./plan-onboarding.component.scss"
})
export class PlanOnboardingComponent implements OnDestroy {

  protected readonly icons = icons;
  protected readonly invite$ = new BehaviorSubject<ShiftPlanInviteDetailsDto | null | "INVALID">(null);
  protected readonly inviteMode$;

  private readonly _pageService = inject(PageService);
  private readonly _planService = inject(ShiftPlanInviteEndpointService);
  private readonly _activatedRoute = inject(ActivatedRoute);
  private readonly _userService = inject(UserService);
  private readonly _router = inject(Router);
  private readonly _toastService = inject(ToastService);

  private readonly _inviteSubscription?: Subscription;

  constructor() {
    const inviteCode = this._activatedRoute.snapshot.paramMap.get("shiftPlanInvite");
    if(inviteCode === null) {
      this._router.navigateByUrl("/");
      return;
    }

    const accept = this._activatedRoute.snapshot.queryParamMap.get("accept");

    this._planService.getShiftPlanInviteDetails(inviteCode).subscribe({
      next: details => {
        this.invite$.next(details);
        this._pageService
          .configurePageName(`Join ${details.eventDto.name}`)
          .configureBreadcrumb(
            BC_EVENT,
            details.eventDto.name,
            details.eventDto.id
          )
          .configureBreadcrumb(
            BC_PLAN_ONBOARDING,
            "Onboarding",
            "/onboarding/" + inviteCode
          );
      },
      error: () => {
        this.invite$.next("INVALID");
      }
    });

    this.inviteMode$ = this.invite$.pipe(
      map(invite => {
        if(invite === null) {return null;}
        if(invite === "INVALID") {return "INVALID";}
        if(!invite.joined) {return "JOIN";}
        if(invite.upgradeToPlannerPossible) {return "UPGRADE_ACCESS";}
        if(invite.extensionOfRolesPossible) {return "UPGRADE_ROLES";}
        return "JOINED";
      })
    );

    this._inviteSubscription = this.inviteMode$.pipe(
      withLatestFrom(this.invite$),
      combineLatestWith(this._userService.userProfile$),
      filter(([[mode], user]) =>
        (mode === "JOIN" || mode === "UPGRADE_ACCESS" || mode === "UPGRADE_ROLES") && accept !== null && user !== null),
      take(1),
      map(([[mode, invite]]) => [mode, invite] as const)
    ).subscribe(([mode, invite]) => {
      if(invite !== "INVALID" && invite !== null && accept !== null &&
        (mode === "JOIN" || mode === "UPGRADE_ACCESS" || mode === "UPGRADE_ROLES")) {
        this.joinShiftPlan(invite);
      }
    });
  }

  ngOnDestroy() {
    this._inviteSubscription?.unsubscribe();
  }

  joinShiftPlan(invite: ShiftPlanInviteDetailsDto) {
    this._userService.kcProfile$.pipe(
      switchMap(profile => {
        if(profile === null) {
          this._userService.login(window.location.href + "?accept=true");
        }

        return this._planService.joinShiftPlan({
          inviteCode: invite.inviteDto.code
        }).pipe(
          this._toastService.tapSuccess("Joined Shift Plan",
            () => `You have joined the shift plan "${invite.inviteDto.shiftPlanDto.name}"`),
          this._toastService.tapError("Error joining shift plan", mapValue.apiErrorToMessage),
          switchMap(() => this._userService.refreshProfile())
        );
      })
    ).subscribe((profile) => {
      console.log("Joined shift plan, navigating to event", profile);
      this._router.navigateByUrl(`/events/${invite.eventDto.id}`);
    });
  }
}
