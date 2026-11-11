import {Component, inject, OnDestroy} from "@angular/core";
import {PageService} from "../../../../services/page/page.service";
import { icons } from "../../../../util/icons";
import {FormBuilder,  ReactiveFormsModule} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {
  EventEndpointService,
  EventShiftPlansOverviewDto, RoleEndpointService,
  ShiftPlanDto,
  ShiftPlanInviteEndpointService
} from "../../../../../shiftservice-client";
import {
  BehaviorSubject,
  combineLatestWith,
  filter,
  map, of,
  shareReplay,
  startWith,
  switchMap,
  take,
  tap, withLatestFrom,
} from "rxjs";
import {BC_EVENT} from "../../../../breadcrumbs";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../../../components/inputs/input-multitoggle/input-multi-toggle.component";
import {AsyncPipe} from "@angular/common";
import {InputSelectComponent, SelectOptions} from "../../../../components/inputs/input-select/input-select.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {TypedFormControlDirective} from "../../../../directives/typed-form-control.directive";
import {ManageInviteComponent} from "../../../../components/manage-invite/manage-invite.component";
import {ManageRoleComponent} from "../../../../components/manage-role/manage-role.component";
import {ManageAssignmentsComponent} from "../../../../components/manage-assignments/manage-assignments.component";
import {FormRouteSyncService} from "../../../../services/form-route-sync.service";
import {ManagePlanVolunteersComponent} from "../../../../components/manage-plan-volunteers/manage-plan-volunteers.component";
import {TooltipDirective} from "../../../../directives/tooltip.directive";
import {ManagePlanStatusComponent} from "../../../../components/manage-plan-status/manage-plan-status.component";

export type managementMode = "invites" | "assignments" | "users" | "roles" | "status";

@Component({
  selector: "app-manage-shift-plans",
  imports: [
    AsyncPipe,
    FaIconComponent,
    InputSelectComponent,
    TypedFormControlDirective,
    ReactiveFormsModule,
    InputMultiToggleComponent,
    ManageInviteComponent,
    ManageRoleComponent,
    ManageAssignmentsComponent,
    ManagePlanVolunteersComponent,
    TooltipDirective,
    ManagePlanStatusComponent
  ],
  templateUrl: "./manage-shift-plans.component.html",
  styleUrl: "./manage-shift-plans.component.scss"
})
export class ManageShiftPlansComponent implements OnDestroy {

  protected readonly form;
  protected readonly event$ = new BehaviorSubject<EventShiftPlansOverviewDto | undefined>(undefined);
  protected readonly planManageData$;
  protected readonly invitesManageData$;
  protected readonly rolesManageData$;
  protected readonly volunteersManageData$;
  protected readonly shiftPlanOptions$;
  protected readonly mode$;
  protected readonly selectedMode$;
  protected readonly icons = icons;

  protected readonly modeOptions: MultiToggleOptions<managementMode> = [
    {name: "Status", value: "status"},
    {name: "Invites", value: "invites"},
    {name: "Roles", value: "roles"},
    {name: "Assignments", value: "assignments"},
    {name: "Volunteers", value: "users"}
  ];

  private readonly _pageService = inject(PageService);
  private readonly _fb = inject(FormBuilder);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _inviteService = inject(ShiftPlanInviteEndpointService);
  private readonly _roleService = inject(RoleEndpointService);
  private readonly _formSyncService = inject(FormRouteSyncService);

  constructor() {
    const eventId = this._route.snapshot.paramMap.get("eventId");
    if(eventId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Event ID is required");
    }

    this.form = this._fb.group({
      shiftPlan: this._fb.nonNullable.control<ShiftPlanDto | null>(null),
      managementMode: this._fb.nonNullable.control<managementMode>("status")
    });

    this._eventService.getShiftPlansOverviewOfEvent(eventId).pipe(
      tap(event => {
        this._pageService
          .configurePageName(`${event.eventOverview.name}`)
          .withCalendarLink("../calendar")
          .configureBreadcrumb(BC_EVENT, event.eventOverview.name, event.eventOverview.id);
      })
    ).subscribe(data => this.event$.next(data));

    this.shiftPlanOptions$ = this.event$.pipe(
      filter(event => event !== undefined),
      map(event => event.shiftPlans
        .sort((a,b) => a.name.localeCompare(b.name))
        .map(plan => ({name: plan.name, value: plan})) as SelectOptions<ShiftPlanDto>
      ),
      shareReplay()
    );

    /* load plan when selection changes */
    this.planManageData$ = this.event$.pipe(
      filter(event => event !== undefined),
      combineLatestWith(this.form.controls.shiftPlan.valueChanges.pipe(
        startWith(this.form.controls.shiftPlan.value)
      )),
      map(([event, shiftPlan]) => ({
        plan: event.shiftPlans.find(plan => plan.id === shiftPlan?.id),
        eventId: event.eventOverview.id
      })),
      shareReplay()
    );

    this.mode$ = this.form.controls.managementMode.valueChanges.pipe(
      startWith(this.form.controls.managementMode.value),
      shareReplay()
    );

    this.selectedMode$ = this.mode$.pipe(
      map(value => this.modeOptions.find(mode => mode.value === value)?.name)
    );

    this._formSyncService.registerForm(
      "manage-shift-plans",
      this.form,
      form => ({
        planId: form.controls.shiftPlan.value?.id ?? "",
        mode: form.controls.managementMode.value
      }),
      params => this.shiftPlanOptions$.pipe(
        withLatestFrom(this.planManageData$, this.mode$), /* just to instantly subscribe (exec) plan manage data */
        take(1),
        map(([options]) => ({
            managementMode: params.mode,
            shiftPlan: options.find(option => option.value.id === params.planId)?.value ?? null,
          })
        ))
    );

    /* fetch invites when plan changes */
    this.invitesManageData$ = this.planManageData$.pipe(
      switchMap(planData => {
          const plan = planData.plan;
          if (plan === undefined) {
            return of(undefined);
          }

          return this._inviteService.getAllShiftPlanInvites(plan.id).pipe(
            map(invites => [
              {plan: plan, invite: undefined},
              ...invites.map(invite => ({plan: plan, invite: invite}))
            ])
          );
        }
      )
    );

    /* fetch roles when plan changes for volunteer management */
    this.volunteersManageData$ = this.planManageData$.pipe(
      switchMap(planData => {
        const plan = planData?.plan;
        if(plan === undefined) {
          return of(undefined);
        }

        return this._roleService.getRoles(plan.id).pipe(
          map(roles => ({
              plan,
              roles: roles
            })
          )
        );
      })
    );

    /* fetch roles when plan changes */
    this.rolesManageData$ = this.planManageData$.pipe(
      switchMap(planData => {
          const plan = planData.plan;
          if (plan === undefined) {
            return of(undefined);
          }

          return this._roleService.getRoles(plan.id).pipe(
            map(roles => [
              {plan: plan, role: undefined},
              ...roles
                .sort((a,b) => a.name.localeCompare(b.name))
                .map(role => ({plan: plan, role: role}))
            ])
          );
        }
      )
    );
  }

  ngOnDestroy() {
    this._formSyncService.unregisterForm("manage-shift-plans");
  }

  protected idComparatorFn(a: {id: string} | null, b: {id: string} | null): boolean {
    return a?.id === b?.id;
  }

  protected refreshData() {
    this.event$.pipe(
      take(1),
      switchMap(event =>
        event === undefined ? of(undefined) :
          this._eventService.getShiftPlansOverviewOfEvent(event?.eventOverview.id)
      )
    ).subscribe(event => this.event$.next(event));
  }
}
