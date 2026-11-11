import {TestBed} from "@angular/core/testing";
import {convertToParamMap, ActivatedRoute, Router} from "@angular/router";
import {BehaviorSubject, firstValueFrom, Observable, of, OperatorFunction} from "rxjs";
import {EventComponent} from "./event.component";
import {PageService} from "../../../services/page/page.service";
import {ToastService} from "../../../services/toast/toast.service";
import {UserService} from "../../../services/user/user.service";
import {
  EventEndpointService,
  EventShiftPlansOverviewDto,
  LeaderBoardDto,
  LeaderboardEndpointService,
  UserEventEndpointService,
  UserEventDto,
  UserProfileDto
} from "../../../../shiftservice-client";
import {ShiftPlanDto} from "../../../../shiftservice-client/model/shift-plan-dto";

describe("EventComponent", () => {
  const eventId = "event-1";
  const eventOverview = createEventOverview(eventId, "Test Event");
  const event = createEventOverviewDto(eventOverview, ["plan-1", "plan-2"]);

  let component: EventComponent;
  let userProfile$: BehaviorSubject<UserProfileDto | null>;
  let pageService: jasmine.SpyObj<PageService>;
  let eventService: jasmine.SpyObj<EventEndpointService>;
  let userEventService: jasmine.SpyObj<UserEventEndpointService>;
  let userService: {
    userProfile$: Observable<UserProfileDto | null>;
    isPlatformAdmin$: Observable<boolean>;
    canManagePlan$: jasmine.Spy;
    refreshProfile: jasmine.Spy;
  };
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    userProfile$ = new BehaviorSubject<UserProfileDto | null>(createProfile({
      platformAdmin: true,
      planningPlans: ["plan-existing"],
      volunteeringPlans: ["vol-existing"],
      planningEvents: [],
      volunteeringEvents: []
    }));

    pageService = jasmine.createSpyObj<PageService>("PageService", [
      "configurePageName",
      "withCalendarLink",
      "configureBreadcrumb"
    ]);
    pageService.configurePageName.and.returnValue(pageService);
    pageService.withCalendarLink.and.returnValue(pageService);
    pageService.configureBreadcrumb.and.returnValue(pageService);

    eventService = jasmine.createSpyObj<EventEndpointService>("EventEndpointService", [
      "getShiftPlansOverviewOfEvent"
    ]);
    eventService.getShiftPlansOverviewOfEvent.and.returnValue(
      of(event) as unknown as ReturnType<EventEndpointService["getShiftPlansOverviewOfEvent"]>
    );

    userEventService = jasmine.createSpyObj<UserEventEndpointService>("UserEventEndpointService", [
      "updateUserPlans"
    ]);
    userEventService.updateUserPlans.and.returnValue(
      of({} as UserEventDto) as unknown as ReturnType<UserEventEndpointService["updateUserPlans"]>
    );

    userService = {
      userProfile$: userProfile$.asObservable(),
      isPlatformAdmin$: of(true),
      canManagePlan$: jasmine.createSpy("canManagePlan$").and.returnValue(of(false)),
      refreshProfile: jasmine.createSpy("refreshProfile").and.returnValue(of(createProfile({
        platformAdmin: true,
        planningPlans: ["plan-existing", "plan-1", "plan-2"],
        volunteeringPlans: ["vol-existing", "plan-1", "plan-2"],
        planningEvents: [eventId],
        volunteeringEvents: [eventId]
      })))
    };

    toastService = jasmine.createSpyObj<ToastService>("ToastService", ["tapSaving", "tapDeleting"]);
    toastService.tapSaving.and.callFake(() => passthroughOperator());
    toastService.tapDeleting.and.callFake(() => passthroughOperator());

    await TestBed.configureTestingModule({
      imports: [EventComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({eventId})
            }
          }
        },
        {
          provide: Router,
          useValue: jasmine.createSpyObj<Router>("Router", ["navigateByUrl"])
        },
        {
          provide: PageService,
          useValue: pageService
        },
        {
          provide: EventEndpointService,
          useValue: eventService
        },
        {
          provide: LeaderboardEndpointService,
          useValue: {
            getLeaderboardForEvent: jasmine.createSpy("getLeaderboardForEvent").and.returnValue(
              of({} as LeaderBoardDto) as unknown as ReturnType<LeaderboardEndpointService["getLeaderboardForEvent"]>
            )
          }
        },
        {
          provide: UserService,
          useValue: userService
        },
        {
          provide: UserEventEndpointService,
          useValue: userEventService
        },
        {
          provide: ToastService,
          useValue: toastService
        }
      ]
    })
      .overrideComponent(EventComponent, {
        set: {
          template: ""
        }
      })
      .compileComponents();

    component = TestBed.createComponent(EventComponent).componentInstance;
  });

  it("offers platform admins a planner join action when they are not yet in every plan", async () => {
    const internals = component as unknown as {
      canJoinEventAsPlanner$: Observable<boolean>;
    };

    const canJoin = await firstValueFrom(internals.canJoinEventAsPlanner$);

    expect(canJoin).toBeTrue();
  });

  it("offers platform admins a leave action when they currently belong to event plans", async () => {
    const internals = component as unknown as {
      canLeaveEventMembership$: Observable<boolean>;
    };

    const canLeave = await firstValueFrom(internals.canLeaveEventMembership$);

    expect(canLeave).toBeTrue();
  });

  it("joins an event as planner by adding every event plan to both memberships", () => {
    const profile = createProfile({
      platformAdmin: true,
      planningPlans: ["plan-existing", "plan-1"],
      volunteeringPlans: ["vol-existing"],
      planningEvents: [],
      volunteeringEvents: []
    });
    const internals = component as unknown as {
      joinEventAsPlanner(event: EventShiftPlansOverviewDto, profile: UserProfileDto): void;
    };

    internals.joinEventAsPlanner(event, profile);

    expect(userEventService.updateUserPlans).toHaveBeenCalledOnceWith("volunteer-1", {
      planningPlans: ["plan-existing", "plan-1", "plan-2"],
      volunteeringPlans: ["vol-existing", "plan-1", "plan-2"]
    });
    expect(toastService.tapSaving).toHaveBeenCalled();
    expect(userService.refreshProfile).toHaveBeenCalledOnceWith();
  });

  it("leaves event membership by removing every event plan from both memberships", () => {
    const profile = createProfile({
      platformAdmin: true,
      planningPlans: ["plan-1", "plan-2", "plan-keep"],
      volunteeringPlans: ["plan-2", "plan-keep"],
      planningEvents: [eventId],
      volunteeringEvents: [eventId]
    });
    const internals = component as unknown as {
      leaveEventMembership(event: EventShiftPlansOverviewDto, profile: UserProfileDto): void;
    };

    internals.leaveEventMembership(event, profile);

    expect(userEventService.updateUserPlans).toHaveBeenCalledOnceWith("volunteer-1", {
      planningPlans: ["plan-keep"],
      volunteeringPlans: ["plan-keep"]
    });
    expect(toastService.tapDeleting).toHaveBeenCalled();
    expect(userService.refreshProfile).toHaveBeenCalledOnceWith();
  });
});

function createEventOverview(id: string, name: string) {
  return {
    id,
    name,
    startTime: "2026-06-24T08:00:00Z",
    endTime: "2026-06-24T18:00:00Z",
    active: true,
    socialMediaLinks: []
  };
}

function createEventOverviewDto(
  overview: ReturnType<typeof createEventOverview>,
  shiftPlanIds: string[]
): EventShiftPlansOverviewDto {
  return {
    eventOverview: overview,
    ownEventStatistics: {} as EventShiftPlansOverviewDto["ownEventStatistics"],
    overallEventStatistics: {} as EventShiftPlansOverviewDto["overallEventStatistics"],
    rewardPoints: 0,
    shiftPlans: shiftPlanIds.map(id => createShiftPlan(id)),
    roles: []
  };
}

function createShiftPlan(id: string): ShiftPlanDto {
  return {
    id,
    name: `Plan ${id}`,
    lockStatus: ShiftPlanDto.LockStatusEnum.SelfSignup,
    defaultNoRolePointsPerMinute: 0
  };
}

function createProfile({
  platformAdmin,
  planningPlans,
  volunteeringPlans,
  planningEvents,
  volunteeringEvents
}: {
  platformAdmin: boolean;
  planningPlans: string[];
  volunteeringPlans: string[];
  planningEvents: string[];
  volunteeringEvents: string[];
}): UserProfileDto {
  return {
    account: {
      volunteer: {
        id: "volunteer-1",
        firstName: "Ada",
        lastName: "Admin"
      },
      username: "ada",
      email: "ada@example.com",
      platformAdmin
    },
    notifications: [],
    assignedRoles: [],
    planningPlans,
    volunteeringPlans,
    planningEvents,
    volunteeringEvents
  };
}

function passthroughOperator<T>(): OperatorFunction<T, T> {
  return source => source;
}
