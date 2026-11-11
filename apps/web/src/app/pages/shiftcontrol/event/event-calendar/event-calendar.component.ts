import {Component, inject, OnDestroy, viewChild} from "@angular/core";
import {calendarConfig, ShiftCalendarGridComponent} from "../../../../components/shift-calendar-grid/shift-calendar-grid.component";
import {EventCalendarFilterComponent} from "../../../../components/event-calendar-filter/event-calendar-filter.component";
import {PageService} from "../../../../services/page/page.service";
import {BC_EVENT} from "../../../../breadcrumbs";
import {ActivatedRoute, Router} from "@angular/router";
import {
  ActivityDto, ActivityScheduleDto,
  EventEndpointService, EventScheduleContentDto, EventScheduleEndpointService, EventScheduleLayoutDto, LocationDto, LocationEndpointService,
  ShiftColumnDto,
  ShiftDto
} from "../../../../../shiftservice-client";
import {
  BehaviorSubject,
  catchError,
  combineLatestWith,
  debounceTime,
  distinctUntilChanged,
  EMPTY,
  filter,
  forkJoin,
  map, merge, Observable,
  of,
  shareReplay,
  startWith, Subject,
  Subscription,
  switchMap, take,
  withLatestFrom,
} from "rxjs";
import {toObservable} from "@angular/core/rxjs-interop";
import {mapValue} from "../../../../util/value-maps";
import {AsyncPipe} from "@angular/common";
import {DialogComponent} from "../../../../components/dialog/dialog.component";
import {ManageShiftComponent, manageShiftParams} from "../../../../components/manage-shift/manage-shift.component";
import {UserService} from "../../../../services/user/user.service";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../../../components/inputs/input-multitoggle/input-multi-toggle.component";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {TypedFormControlDirective} from "../../../../directives/typed-form-control.directive";
import {ManageActivityComponent, manageActivityParams} from "../../../../components/manage-activity/manage-activity.component";

type viewMode = "shift" | "activity";

@Component({
  selector: "app-event-calendar",
  imports: [
    ShiftCalendarGridComponent,
    EventCalendarFilterComponent,
    AsyncPipe,
    DialogComponent,
    ManageShiftComponent,
    InputMultiToggleComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    ManageActivityComponent
  ],
  standalone: true,
  templateUrl: "./event-calendar.component.html",
  styleUrl: "./event-calendar.component.scss"
})
export class EventCalendarComponent implements OnDestroy {

  protected selectedShift$ = new BehaviorSubject<manageShiftParams | undefined>(undefined);
  protected selectedActivity$ = new BehaviorSubject<manageActivityParams | undefined>(undefined);
  protected activityChanged$ = new Subject<ActivityDto>();
  protected shiftChanged$ = new Subject<ShiftDto>();
  protected readonly form;
  protected readonly calendarModeOptions: MultiToggleOptions<viewMode> = [
    {name: "Shifts", value: "shift"},
    {name: "Schedule", value: "activity"}
  ];

  private readonly _shiftPlanSchedule = viewChild(ShiftCalendarGridComponent);
  private readonly _filterComponent = viewChild(EventCalendarFilterComponent);

  private readonly _subscriptions: Subscription[];

  private readonly _fb = inject(FormBuilder);
  private readonly _pageService = inject(PageService);
  private readonly _eventScheduleService = inject(EventScheduleEndpointService);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _locationsService = inject(LocationEndpointService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _userService = inject(UserService);

  constructor() {
    const eventId = this._route.snapshot.paramMap.get("eventId");
    if(eventId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Event ID is required");
    }

    this.form = this._fb.group({
      calendarMode: this._fb.nonNullable.control<viewMode>("shift")
    });

    this._subscriptions = this.setupWithObservables(eventId);
  }

  protected get isPlatformAdmin$(){
    return this._userService.isPlatformAdmin$;
  }

  ngOnDestroy() {
    this._subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  private setupWithObservables(eventId: string): Subscription[]{

    /* details about the selected event */
    const event$ = this._eventService.getShiftPlansOverviewOfEvent(eventId).pipe(
      catchError(() => {
        this._router.navigateByUrl("/");
        return EMPTY;
      }),
      shareReplay()
    );

    const isAdmin$ = this._userService.isPlatformAdmin$.pipe(
      shareReplay()
    );

    const isPlanner$ = event$.pipe(
      switchMap(event => forkJoin(
        event.shiftPlans.map(plan => this._userService.canManagePlan$(plan.id).pipe(take(1)))
      )),
      map(canManageList => canManageList.some(canManage => canManage)),
      startWith(false),
      shareReplay()
    );

    /* activity / shift mode */
    const calendarMode$ = this.form.controls.calendarMode.valueChanges.pipe(
      startWith(this.form.controls.calendarMode.value),
      distinctUntilChanged(),
      debounceTime(50),
      shareReplay()
    );

    /* observable with locations */
    const locations$ = this._locationsService.getAllLocationsForEvent(eventId).pipe(
      shareReplay()
    );

    /* observable based on the filter component view child signal */
    const filterComponent$ = toObservable(this._filterComponent).pipe(
      filter(filterComponent => filterComponent !== undefined),
      shareReplay()
    );

    /* observable containing form filter values */
    const filters$ = filterComponent$.pipe(
      switchMap(component => component?.searchForm?.valueChanges ?? of(undefined)),
      filter(data => data !== undefined),
      debounceTime(200),
      startWith({} as Partial<EventCalendarFilterComponent["searchForm"]["value"]>)
    );

    /* observable containing form view options values */
    const viewOptions$ = filterComponent$.pipe(
      map(component => component?.viewForm ?? undefined),
      startWith(undefined)
    );

    /* observable containing the current calendar layout config */
    const layout$: Observable<EventScheduleLayoutDto> = calendarMode$.pipe(
      combineLatestWith(merge(this.activityChanged$, this.shiftChanged$).pipe(
        startWith(undefined)
      )),
      switchMap(([mode]) => mode === "activity" ?

        /* schedule mode */
        locations$.pipe(
          map(locations => locations.map(location => ({location, requiredShiftColumns: 0}))),
          map(locationLayouts => ({
            scheduleLayoutDtos: locationLayouts,
            scheduleStatistics: {
              totalShifts: 0,
              totalHours: 0,
              unassignedCount: 0,
              shiftCountForSignup: 0
            },
            scheduleLayoutNoLocationDto: {
              requiredShiftColumns: 0
            }
          }))
        ) :

          /* shifts mode */
          filters$.pipe(
            switchMap(filters =>
            this._eventScheduleService.getEventScheduleLayout(
              eventId,
              undefined,
              mapValue.undefinedIfEmptyString(filters?.shiftName),
              filters?.relevanceList,
              filters?.rolesList,
              filters?.locationsList,
              "body"
            )),
          catchError(() => {
            this._router.navigateByUrl("/");
            return EMPTY;
          })
        )),
      shareReplay()
    );

    /* observable based on the calendar view child signal */
    const calendarComponent$ = toObservable(this._shiftPlanSchedule).pipe(
      filter(calendar => calendar !== undefined)
    );

    /* observable containing the currently navigated-to date of the calendar */
    const calendarNavigation$ = calendarComponent$.pipe(
      switchMap(calendar => calendar.navigation$),
      distinctUntilChanged((prev, curr) =>
        prev.visibleDates.map(d => d.getTime()).join(",") === curr.visibleDates.map(d => d.getTime()).join(",")
        && prev.cachedDates.map(d => d.getTime()).join(",") === curr.cachedDates.map(d => d.getTime()).join(",")
      )
    );

    /* observable containing the filter component configuration */
    const filterData$ = this._eventScheduleService.getEventScheduleFilterValues(eventId).pipe(
      shareReplay()
    );

    const subs: Subscription[] = [];

    /* toggle filter mode depending on calendar mode */
    subs.push(calendarMode$.pipe(
      withLatestFrom(filterComponent$)
    ).subscribe(([mode, filterComponent]) => {
      if(filterComponent !== undefined) {
        filterComponent.showShiftFilterForm = mode === "shift";
      }
    }));

    /* Update page properties */
    subs.push(event$.subscribe(dashboard => {
      this._pageService
        .configurePageName(`${dashboard.eventOverview.name} Calendar`)
        .withCalendarLink("./")
        .configureBreadcrumb(
          BC_EVENT,
          dashboard.eventOverview.name,
          dashboard.eventOverview.id
        );
    }));

    /* set filter values with api data */
    subs.push(filterComponent$.pipe(
      combineLatestWith(filterData$, event$)
    ).subscribe(([filterComponent, filterData, event]) => {
      filterComponent.rolesOptions = filterData.roles
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(role => ({name: role.name, value: role.id}));
      filterComponent.locationsOptions = filterData.locations
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(location => ({name: location.name, value: location.id}));
      filterComponent.plansOptions = event.shiftPlans
        .sort((a, b) => a.name.localeCompare(b.name))
        .map(plan => ({name: plan.name, value: plan.id}));
    }));

    /* react to calendar config and set in component */
    subs.push(filterData$.pipe(
      combineLatestWith(calendarComponent$, layout$, filterComponent$, calendarMode$),
      withLatestFrom(calendarNavigation$.pipe(
        startWith({visibleDates: [], cachedDates: []})
      ), event$, isAdmin$, isPlanner$)
    ).subscribe((
      [[filterData, calendar, layout, filterComponent, calendarMode],
        navigation, event, isAdmin, isPlanner
      ]) => {

      /*
      start date parsed from utc date, begin of day
      */
      const startDate = filterData.firstDate ?
        mapValue.localDayBeginFromDatetimeString(filterData.firstDate) : new Date();

      /* end date parsed from utc date, end of day */
      const endDate = filterData.lastDate ?
        mapValue.localDayEndFromDatetimeString(filterData.lastDate) : new Date();

      const config: calendarConfig = {
        startDate,
        endDate,
        eventStartTime: new Date(event.eventOverview.startTime),
        eventEndTime: new Date(event.eventOverview.endTime),
        showNoContentHint: event.shiftPlans.length === 0,
        activityWidth: calendarMode === "activity" ? "minmax(5rem, 1fr)" : undefined,
        locationLayouts: layout.scheduleLayoutDtos,
        noLocationLayout: layout.scheduleLayoutNoLocationDto,
        shiftPaddingColumn: calendarMode === "shift",
        shiftClickCallback: calendarMode === "activity" ? undefined : shift => {
          this.selectedShift$.next({
            shift,
            eventId: event.eventOverview.id
          });
        },
        activityClickCallback: calendarMode === "shift" ? undefined : activity => {
          this.selectedActivity$.next({
            eventId: event.eventOverview.id,
            activity
          });
        },
        emptySpaceClickCallback: calendarMode === "shift" ?

          /* shift mode*/
          (!isPlanner ? undefined : (date, location) => this.selectedShift$.next({
            eventId: event.eventOverview.id,
            suggestedLocation: location,
            shift: undefined,
            suggestedDate: date
          })) :

          /* activity mode */
          (!isAdmin ? undefined : (date, location) => {
              this.selectedActivity$.next({
                suggestedDate: date,
                suggestedLocation: location,
                eventId: event.eventOverview.id
              });
            }
          )
      };
      const calendarViewInited = navigation.visibleDates.length > 0;
      calendar.setConfig(config);

      filterComponent.statistics = layout.scheduleStatistics;
      filterComponent.statisticsMode = isAdmin ? "admin" : isPlanner ? "planner" : "volunteer";

      if(!calendarViewInited) {
        calendar.jumpToDate(new Date(Math.min(Math.max(startDate.getTime(), new Date().getTime()), endDate.getTime())));
      }
    }));

    /* set date picker to date when changed */
    subs.push(calendarNavigation$.pipe(
      withLatestFrom(viewOptions$)
    ).subscribe(([navigation, viewOptions]) => {
      if(viewOptions === undefined) {
        return;
      }
      const firstDay = [...navigation.visibleDates].shift();
      if(firstDay === undefined) {
        return;
      }

      viewOptions.controls.date.setValue(firstDay);
    }));

    /* subscribe to date picker changes and jump to date in calendar */
    subs.push(viewOptions$.pipe(
      switchMap(view => view ? view.controls.date.valueChanges : of(null)),
      withLatestFrom(calendarNavigation$.pipe(
        map(nav => [...nav.visibleDates].shift()),
        startWith(undefined)
      ), calendarComponent$, calendarComponent$.pipe(
        switchMap(calendar => calendar?.scrolling$ ?? of(false))
      ))
    ).subscribe(([pickerDate, currentDate, calendar, scrolling]) => {
      if(pickerDate === null || currentDate === undefined || scrolling) {
        return;
      }

      if(pickerDate.getTime() !== currentDate.getTime()) {
        calendar.jumpToDate(pickerDate);
      }
    }));

    /* toggle filters when clicked in calendar */
    subs.push(calendarComponent$.pipe(
      switchMap(calendar => calendar.filterToggled$),
      withLatestFrom(toObservable(this._filterComponent))
    ).subscribe(([,filterComponent]) => {
      if(filterComponent !== undefined) {
        filterComponent.showFilters = !filterComponent.showFilters;
      }
    }));

    /* load schedule when filter or navigated date changes */
    subs.push(filters$.pipe(
      combineLatestWith(calendarComponent$, filterData$, calendarNavigation$, calendarMode$),
      switchMap(([filters, calendar, , navigation, calendarMode]) =>{
        const newDays = navigation.visibleDates.filter(visible =>
          !navigation.cachedDates.some(cached => cached.getTime() === visible.getTime())
        );

        const scheduleDays = calendarMode === "shift" ?

          /* shift schedule mode */
          newDays.map(date => this._eventScheduleService.getEventScheduleContent(
            eventId,
            mapValue.datetimeToUtcDateString(date),
            filters?.plansList,
            mapValue.undefinedIfEmptyString(filters?.shiftName),
            filters?.relevanceList,
            filters?.rolesList,
            filters?.locationsList,
            "body"
          )) :

          /* event schedule mode */
          newDays.map(date => this._eventScheduleService.getActivitySchedule(
            eventId,
            mapValue.datetimeToUtcDateString(date),
            "body"
          ).pipe(
            map(schedule => this.mapActivityScheduleToEventSchedule(schedule, date))
          ));

        return forkJoin(scheduleDays).pipe(
          map(schedules => ({calendar, schedules}))
        );
      })
    ).subscribe(({calendar, schedules}) => {
      calendar.addScheduleDays(...schedules);
    }));

    return subs;
  }

  private mapActivityScheduleToEventSchedule(schedule: ActivityScheduleDto, date: Date): EventScheduleContentDto {
    const locationMap = new Map<string, ActivityDto[]>();
    const locationIdMap = new Map<string, LocationDto>();
    const activitiesNoLocation: ActivityDto[] = [];

    schedule.activities.forEach(activity => {
      if(activity.location === undefined) {
        activitiesNoLocation.push(activity);
        return;
      }

      locationIdMap.set(activity.location.id, activity.location);

      if(!locationMap.has(activity.location.id)) {
        locationMap.set(activity.location.id, []);
      }
      locationMap.get(activity.location.id)?.push(activity);
    });

    return {
      date: mapValue.datetimeToUtcDateString(date),
      scheduleContentDtos: [...locationIdMap.entries()]
        .map(([id, location]) => ({
          location,
          activities: locationMap.get(id) ?? [],
          shiftColumns: [] as ShiftColumnDto[]
        })),
      scheduleContentNoLocationDto: {
        activities: activitiesNoLocation,
        shiftColumns: [] as ShiftColumnDto[]
      }
    } as EventScheduleContentDto;
  }
}
