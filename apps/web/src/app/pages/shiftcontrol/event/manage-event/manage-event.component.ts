import {Component, inject} from "@angular/core";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../../../components/inputs/input-multitoggle/input-multi-toggle.component";
import { icons } from "../../../../util/icons";
import {PageService} from "../../../../services/page/page.service";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {EventEndpointService, EventShiftPlansOverviewDto, LocationEndpointService} from "../../../../../shiftservice-client";
import {BehaviorSubject, combineLatestWith, map, of, shareReplay, startWith, switchMap, take, tap} from "rxjs";
import {BC_EVENT} from "../../../../breadcrumbs";
import {AsyncPipe} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {TypedFormControlDirective} from "../../../../directives/typed-form-control.directive";
import {ManagePlanDetailsComponent} from "../../../../components/manage-plan-details/manage-plan-details.component";
import {ManageEventDetailsComponent} from "../../../../components/manage-event-details/manage-event-details.component";
import {ManageLocationComponent} from "../../../../components/manage-location/manage-location.component";
import {EventExportComponent} from "../../../../components/event-export/event-export.component";

export type managementMode = "details" | "locations" | "plans" | "export";

@Component({
  selector: "app-manage-event",
  imports: [
    AsyncPipe,
    FaIconComponent,
    InputMultiToggleComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    ManagePlanDetailsComponent,
    ManageEventDetailsComponent,
    ManageLocationComponent,
    RouterLink,
    EventExportComponent
  ],
  templateUrl: "./manage-event.component.html",
  styleUrl: "./manage-event.component.scss"
})
export class ManageEventComponent {
  protected readonly form;
  protected readonly event$ = new BehaviorSubject<EventShiftPlansOverviewDto | undefined>(undefined);
  protected readonly planManageData$;
  protected readonly locationsManageData$;
  protected readonly mode$;
  protected readonly selectedMode$;
  protected readonly icons = icons;

  protected readonly modeOptions: MultiToggleOptions<managementMode> = [
    {name: "Details", value: "details"},
    {name: "Export", value: "export"},
    {name: "Shift Plans", value: "plans"},
    {name: "Locations", value: "locations"}
  ];

  private readonly _pageService = inject(PageService);
  private readonly _fb = inject(FormBuilder);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _locationService = inject(LocationEndpointService);

  constructor() {
    const eventId = this._route.snapshot.paramMap.get("eventId");
    if(eventId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Event ID is required");
    }

    this.form = this._fb.group({
      managementMode: this._fb.nonNullable.control<managementMode>("details")
    });

    this._eventService.getShiftPlansOverviewOfEvent(eventId).pipe(
      tap(event => {
        this._pageService
          .configurePageName(`${event.eventOverview.name}`)
          .withCalendarLink("../calendar")
          .configureBreadcrumb(BC_EVENT, event.eventOverview.name, event.eventOverview.id);
      })
    ).subscribe(data => this.event$.next(data));

    this.mode$ = this.form.controls.managementMode.valueChanges.pipe(
      startWith(this.form.controls.managementMode.value),
      shareReplay()
    );

    this.selectedMode$ = this.mode$.pipe(
      map(value => this.modeOptions.find(mode => mode.value === value)?.name)
    );

    /* set plans when event changes */
    this.planManageData$ = this.event$.pipe(
      combineLatestWith(this.mode$),
      map(([eventData, mode]) => {
        if (eventData === undefined || mode !== "plans") {
          return undefined;
        }

        return [
          {plan: undefined, eventId: eventData.eventOverview.id},
          ...eventData.shiftPlans.map(plan => ({plan: plan, eventId: eventData.eventOverview.id}))
        ];
      })
    );

    this.locationsManageData$ = this.event$.pipe(
      combineLatestWith(this.mode$),
      switchMap(([eventData, mode]) => {
        if (eventData === undefined || mode !== "locations") {
          return of(undefined);
        }

        return this._locationService.getAllLocationsForEvent(eventData.eventOverview.id).pipe(
          map(invites => [
            {event: eventData.eventOverview, location: undefined},
            ...invites.map(location => ({event: eventData.eventOverview, location: location}))
          ])
        );
      })
    );
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
