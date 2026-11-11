import {Component, inject} from "@angular/core";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {PageService} from "../../../services/page/page.service";
import {BC_EVENT} from "../../../breadcrumbs";
import {ReactiveFormsModule} from "@angular/forms";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {
  EventEndpointService,
  EventShiftPlansOverviewDto,
  LeaderBoardDto,
  LeaderboardEndpointService,
  SocialMediaLinkDto,
  UserEventEndpointService,
  UserProfileDto
} from "../../../../shiftservice-client";
import {combineLatest, map, Observable, of, shareReplay, switchMap, tap} from "rxjs";
import {AsyncPipe, DatePipe, DecimalPipe} from "@angular/common";
import {TooltipDirective} from "../../../directives/tooltip.directive";
import {UserService} from "../../../services/user/user.service";
import {icons} from "../../../util/icons";
import {EventLeaderboardComponent} from "../../../components/event-leaderboard/event-leaderboard.component";
import {EventRewardsComponent} from "../../../components/event-rewards/event-rewards.component";
import {ToastService} from "../../../services/toast/toast.service";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";

@Component({
  selector: "app-plans",
  imports: [
    RouterLink,
    ReactiveFormsModule,
    FaIconComponent,
    AsyncPipe,
    DatePipe,
    TooltipDirective,
    DecimalPipe,
    EventLeaderboardComponent,
    EventRewardsComponent,
    InputButtonComponent
  ],
  standalone: true,
  templateUrl: "./event.component.html",
  styleUrl: "./event.component.scss"
})
export class EventComponent {

  protected readonly eventId: string;
  protected readonly event$: Observable<EventShiftPlansOverviewDto>;
  protected readonly leaderboard$: Observable<LeaderBoardDto>;
  protected readonly canManageInEvent$;
  protected readonly isPlannerInEvent$;
  protected readonly isParticipantInEvent$;
  protected readonly isPlatformAdmin$;
  protected readonly currentUserProfile$;
  protected readonly canJoinEventAsPlanner$;
  protected readonly canLeaveEventMembership$;

  protected readonly icons = icons;

  private readonly _pageService = inject(PageService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _leaderboardService = inject(LeaderboardEndpointService);
  private readonly _userService = inject(UserService);
  private readonly _userEventService = inject(UserEventEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    const eventId = this._route.snapshot.paramMap.get("eventId");
    if(eventId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Event ID is required");
    }
    this.eventId = eventId;

    this.event$ = this._eventService.getShiftPlansOverviewOfEvent(eventId).pipe(
      tap(event => {
        this._pageService
          .configurePageName(`${event.eventOverview.name}`)
          .withCalendarLink("calendar")
          .configureBreadcrumb(BC_EVENT, event.eventOverview.name, event.eventOverview.id);
      }),
      shareReplay()
    );

    this.leaderboard$ = this._leaderboardService.getLeaderboardForEvent(eventId);
    this.isPlatformAdmin$ = this._userService.isPlatformAdmin$;
    this.currentUserProfile$ = this._userService.userProfile$;
    this.canManageInEvent$ = this.event$.pipe(
      switchMap(event => event.shiftPlans.length === 0
        ? of([false])
        : combineLatest(event.shiftPlans.map(plan => this._userService.canManagePlan$(plan.id)))),
      map(plans => plans.includes(true))
    );
    this.isPlannerInEvent$ = this._userService.userProfile$.pipe(
      map(profile => profile?.planningEvents.some(event => event === this.eventId) === true)
    );
    this.isParticipantInEvent$ = this._userService.userProfile$.pipe(
      map(profile => profile !== null && (
        profile.planningEvents.some(event => event === this.eventId) ||
        profile.volunteeringEvents.some(event => event === this.eventId)
      ))
    );
    this.canJoinEventAsPlanner$ = combineLatest([this.event$, this.currentUserProfile$]).pipe(
      map(([event, profile]) => profile !== null
        && profile.account.platformAdmin === true
        && event.shiftPlans.length > 0
        && event.shiftPlans.some(plan => !profile.planningPlans.includes(plan.id)))
    );
    this.canLeaveEventMembership$ = combineLatest([this.event$, this.currentUserProfile$]).pipe(
      map(([event, profile]) => profile !== null
        && profile.account.platformAdmin === true
        && event.shiftPlans.some(plan =>
          profile.planningPlans.includes(plan.id) || profile.volunteeringPlans.includes(plan.id)))
    );
  }

  public getSocialIcon(social: SocialMediaLinkDto){
    switch(social.type){
      case SocialMediaLinkDto.TypeEnum.Facebook:
        return this.icons.facebook;
      case SocialMediaLinkDto.TypeEnum.Instagram:
        return this.icons.instagram;
      case SocialMediaLinkDto.TypeEnum.Linkedin:
        return this.icons.linkedIn;
      case SocialMediaLinkDto.TypeEnum.Twitter:
        return this.icons.twitter;
      case SocialMediaLinkDto.TypeEnum.Youtube:
        return this.icons.youtube;
      case SocialMediaLinkDto.TypeEnum.Tiktok:
        return this.icons.tiktok;
      case SocialMediaLinkDto.TypeEnum.Discord:
        return this.icons.discord;
      case SocialMediaLinkDto.TypeEnum.Reddit:
        return this.icons.reddit;
      case SocialMediaLinkDto.TypeEnum.X:
        return this.icons.twitter;
      case SocialMediaLinkDto.TypeEnum.Twitch:
        return this.icons.twitch;
      default:
        return this.icons.website;
    }
  }

  public getSocialName(social: SocialMediaLinkDto){
    switch(social.type){
      case SocialMediaLinkDto.TypeEnum.Facebook:
        return "Facebook";
      case SocialMediaLinkDto.TypeEnum.Instagram:
        return "Instagram";
      case SocialMediaLinkDto.TypeEnum.Linkedin:
        return "LinkedIn";
      case SocialMediaLinkDto.TypeEnum.Twitter:
        return "Twitter";
      case SocialMediaLinkDto.TypeEnum.Youtube:
        return "YouTube";
      case SocialMediaLinkDto.TypeEnum.Tiktok:
        return "TikTok";
      case SocialMediaLinkDto.TypeEnum.Discord:
        return "Discord";
      case SocialMediaLinkDto.TypeEnum.Reddit:
        return "Reddit";
      case SocialMediaLinkDto.TypeEnum.X:
        return "X";
      case SocialMediaLinkDto.TypeEnum.Twitch:
        return "Twitch";
      default:
        return "Website";
    }
  }

  protected joinEventAsPlanner(event: EventShiftPlansOverviewDto, profile: UserProfileDto) {
    const eventPlanIds = event.shiftPlans.map(plan => plan.id);
    const planningPlans = Array.from(new Set([...profile.planningPlans, ...eventPlanIds]));
    const volunteeringPlans = Array.from(new Set([...profile.volunteeringPlans, ...eventPlanIds]));

    this.updateCurrentUserPlans(event, profile, planningPlans, volunteeringPlans, "Planner Access", "save");
  }

  protected leaveEventMembership(event: EventShiftPlansOverviewDto, profile: UserProfileDto) {
    const eventPlanIds = new Set(event.shiftPlans.map(plan => plan.id));
    const planningPlans = profile.planningPlans.filter(planId => !eventPlanIds.has(planId));
    const volunteeringPlans = profile.volunteeringPlans.filter(planId => !eventPlanIds.has(planId));

    this.updateCurrentUserPlans(event, profile, planningPlans, volunteeringPlans, "Event Membership", "delete");
  }

  private updateCurrentUserPlans(
    event: EventShiftPlansOverviewDto,
    profile: UserProfileDto,
    planningPlans: string[],
    volunteeringPlans: string[],
    toastLabel: string,
    action: "save" | "delete"
  ) {
    this._userEventService.updateUserPlans(profile.account.volunteer.id, {
      planningPlans,
      volunteeringPlans
    }).pipe(
      action === "save"
        ? this._toastService.tapSaving(toastLabel, () => event.eventOverview.name)
        : this._toastService.tapDeleting(toastLabel, () => event.eventOverview.name),
      switchMap(() => this._userService.refreshProfile())
    ).subscribe();
  }
}
