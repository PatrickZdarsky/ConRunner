import {Component, ElementRef, viewChild} from "@angular/core";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {
  ActivityDto, LocationDto,
  PositionSlotDto,
  ScheduleLayoutDto,
  ShiftColumnDto,
  ShiftDto,
  EventScheduleContentDto
} from "../../../shiftservice-client";
import {AsyncPipe, DatePipe, NgClass} from "@angular/common";
import {
  BehaviorSubject,
  combineLatestWith,
  debounceTime, delayWhen, distinctUntilChanged,
  filter,
  map,
  Observable,
  shareReplay,
  Subject,
  take,
  withLatestFrom
} from "rxjs";
import {toObservable} from "@angular/core/rxjs-interop";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {icons} from "../../util/icons";
import {RouterLink} from "@angular/router";

/**
 * Configuration for the shift calendar grid component.
 * DateTimes are in UTC; calendar needs to calculate necessary date range
 * in order to display whole days starting from 00:00:00
 */
export interface calendarConfig {
  startDate: Date;
  endDate: Date;
  eventStartTime: Date;
  eventEndTime: Date;
  locationLayouts: ScheduleLayoutDto[];
  noLocationLayout?: Omit<ScheduleLayoutDto, "location">;
  activityWidth?: string;
  hideFilterToggle?: boolean;
  emptySpaceClickCallback?: (date: Date, location?: LocationDto) => void;
  activityClickCallback?: (activity: ActivityDto) => void;
  shiftClickCallback?: (shift: ShiftDto) => void;
  shiftPaddingColumn?: boolean;
  showNoContentHint?: boolean;
}

/**
 * Extended calendar configuration with adjusted start and end dates,
 * to ensure full days are displayed to cover full UTC time shift
 */
interface calendarAdjustedConfig extends calendarConfig {
  /**
   * @deprecated use adjusted startDate instead
   */
  startDate: Date;

  /**
   * @deprecated use adjusted endDate instead
   */
  endDate: Date;
  adjustedStartDate: Date;
  adjustedEndDate: Date;
}

/**
 * The navigation state of the calendar.
 * DateTimes are in UTC; in the user timezone, they are at 00:00:00
 */
export interface calendarNavigation {
  visibleDates: Date[];
  cachedDates: Date[];
}

@Component({
  selector: "app-shift-calendar-grid",
  imports: [
    FaIconComponent,
    DatePipe,
    NgClass,
    AsyncPipe,
    InputButtonComponent,
    RouterLink
  ],
  standalone: true,
  templateUrl: "./shift-calendar-grid.component.html",
  styleUrl: "./shift-calendar-grid.component.scss"
})
export class ShiftCalendarGridComponent {

  /**
   * Navigation events of the calendar, when the user scrolls or jumps to a date
   */
  public readonly navigation$: Observable<calendarNavigation>;

  /**
   * The UTC DateTime of the current topmost visible calendar time
   */
  public readonly headDay$: Observable<Date>;

  protected readonly icons = icons;
  protected readonly scrolled$ = new Subject<Event>();
  protected readonly bodyInitialized$ = new BehaviorSubject<boolean>(false);

  /**
   * The lazily loaded schedule days mapped by date string
   * @protected
   */
  protected readonly loadedDays$ = new BehaviorSubject<Map<string, EventScheduleContentDto>>(new Map());

  /**
   * The calendar configuration (externally) based on current filters
   * @protected
   */
  protected readonly config$ = new BehaviorSubject<calendarAdjustedConfig | undefined>(undefined);

  /**
   * The list of shifts and activities across all loaded days,
   * duplicates removed, joined with their locations
   * @protected
   */
  protected readonly schedule$ = this.loadedDays$.pipe(
    map(dayMap => dayMap.size > 0 ? [...dayMap.values()] : []),
    map(days => {
      const activities = days
        .flatMap(day => [
          ...day.scheduleContentDtos.
          map(schedule => schedule.activities.map(activity => ({
              activity,
              location: schedule.location
            }))
          ),
          ...(day.scheduleContentNoLocationDto === undefined ? [] : [day.scheduleContentNoLocationDto])
          .map(schedule => schedule.activities.map(activity => ({
            activity,
            location: undefined
          })))
        ])
        .flat();
      const activityMap = new Map(activities.map(activityItem => [activityItem.activity.id, activityItem]));

      const shifts = days
        .flatMap(day => [
          ...day.scheduleContentDtos.
          map(schedule => schedule.shiftColumns.map(shift => ({
              shift,
              location: schedule.location
            }))
          ),
          ...(day.scheduleContentNoLocationDto === undefined ? [] : [day.scheduleContentNoLocationDto])
          .map(schedule => schedule.shiftColumns.map(shift => ({
            shift,
            location: undefined
          })))
        ])
        .flat();
      const shiftMap = new Map<string, typeof shifts[number]>();
      for (const shiftItem of shifts) {
        const shiftId = shiftItem.shift.shiftDto.id;
        const existingShiftItem = shiftMap.get(shiftId);

        if (
          existingShiftItem === undefined ||
          shiftItem.shift.columnIndex > existingShiftItem.shift.columnIndex
        ) {
          shiftMap.set(shiftId, shiftItem);
        }
      }

      return {
        activities: [...activityMap.values()],
        shifts: [...shiftMap.values()]
      };
    }),
    shareReplay()
  );

  private readonly activityWidth = "2rem";
  private readonly dateWidth = "6rem";
  private readonly shiftWidth = "minmax(10rem, 1fr)";
  private readonly venueGapWidth = "1rem";
  private readonly minuteHeightRem = 0.05;

  private readonly _calendarParent = viewChild<ElementRef<HTMLButtonElement>>("calendarParent");
  private readonly _visibleDates$ = new BehaviorSubject<Date[]>([]);
  private readonly _jumpDate$ = new Subject<Date>();
  private readonly _filterToggled$ = new Subject<void>();
  private readonly _scrolling$ = new BehaviorSubject<boolean>(false);
  constructor() {
    this.navigation$ = this._visibleDates$.pipe(
      withLatestFrom(this.bodyInitialized$.pipe(
        filter(initialized => initialized),
        take(1)
      )),
      filter(([, initialized]) => initialized),
      combineLatestWith(this.loadedDays$),
      map(([[visibleDates], loadedDays]) => {
        const navigation: calendarNavigation = {
          cachedDates: Array.from(loadedDays.keys()).map(dateStr => new Date(dateStr + "T00:00:00Z")),
          visibleDates
        };
        return navigation;
      })
    );

    /* calculate the currently visible days based on scroll position */
    this.scrolled$.pipe(
      debounceTime(10),
      withLatestFrom(this.config$),
      map(([event, config]) => {
        if (config === undefined) {
          return undefined;
        }

        return this.getVisibleValidDays(event.target as HTMLDivElement, config);
      }),
      filter(date => date !== undefined)
    ).subscribe(dates => {
      this._visibleDates$.next(dates);
    });

    /* calculate the currently navigated day based on scroll position */
    this.headDay$ = this.scrolled$.pipe(
      debounceTime(10),
      withLatestFrom(this.config$),
      map(([event, config]) => {
        if (config === undefined) {
          return undefined;
        }

        return this.getScrolledDate(event.target as HTMLDivElement, config);
      }),
      filter(date => date !== undefined)
    );

    /* jump to date when requested by changing the scroll position of the container */
    this._jumpDate$.pipe(
      combineLatestWith(toObservable(this._calendarParent).pipe(
        map((parent => parent?.nativeElement as HTMLElement)),
      )),
      filter(([, parent]) => parent !== undefined),
      withLatestFrom(this.config$),
      delayWhen(() => this.bodyInitialized$.pipe(
          filter(initialized => initialized),
          take(1)
        )
      )
    ).subscribe(([[date, parent], config]) => {

      if(config === undefined) {
        throw new Error("startDate and endDate inputs are required to jump to date");
      }

      const totalMinutes = (config.adjustedEndDate.getTime() - config.adjustedStartDate.getTime()) / (1000 * 60);
      const targetMinutes = (date.getTime() - config.adjustedStartDate.getTime()) / (1000 * 60);
      const scrollPercent = targetMinutes / totalMinutes;

      parent.scrollTo({
        left: parent.scrollLeft,
        top: parent.scrollHeight * scrollPercent,
        behavior: "smooth"
      });

      const visibleDays = this.getVisibleValidDays(parent as HTMLDivElement, config);
      this._visibleDates$.next(visibleDays);
    });
  }

  /**
   * Observable indicating whether the user is currently scrolling the calendar
   */
  public get scrolling$(): Observable<boolean> {
    return this._scrolling$.pipe(
      distinctUntilChanged()
    );
  }

  /**
   * Event emitted when the filter panel is toggled
   */
  public get filterToggled$(): Observable<void> {
    return this._filterToggled$.asObservable();
  }

  /**
   * Add schedule data to the calendar
   * @param schedules
   */
  public addScheduleDays(...schedules: EventScheduleContentDto[]) {
    const currentMap = this.loadedDays$.getValue();
    for (const schedule of schedules) {
      currentMap.set(schedule.date, schedule);
    }
    this.loadedDays$.next(currentMap);
  }

  /**
   * Clear all loaded schedule days
   */
  public clearLoadedDays() {
    this.loadedDays$.next(new Map());
  }

  /**
   * Set the calendar configuration,
   * clearing any previously loaded days
   * @param config
   */
  public setConfig(config: calendarConfig) {
    this.bodyInitialized$.next(false);

    const adjustedConfig: calendarAdjustedConfig = {
      ... config,
      adjustedStartDate: new Date(config.startDate.getTime()),
      adjustedEndDate: new Date(config.endDate.getTime())
    };

    /* adjustedConfig.adjustedStartDate.setHours(
      config.startDate.getUTCHours(),
      config.startDate.getUTCMinutes(),
      config.startDate.getUTCSeconds(),
      config.startDate.getUTCMilliseconds()
    );

    adjustedConfig.adjustedEndDate.setHours(
      config.endDate.getUTCHours(),
      config.endDate.getUTCMinutes(),
      config.endDate.getUTCSeconds(),
      config.endDate.getUTCMilliseconds()
    );*/

    this.config$.next(adjustedConfig);
    this.loadedDays$.next(new Map());
  }

  /**
   * Jump to a specific date in the calendar
   * @param date a datetime in UTC, should represent 00:00:00 in the user timezone
   */
  public jumpToDate(date: Date) {
    this._jumpDate$.next(date);
  }

  /**
   * Handle when the filter toggle button is clicked
   * @protected
   */
  protected filterToggleClicked() {
    this._filterToggled$.next();
  }

  /**
   * Generate the list of hours to display based on the calendar config
   * @param config
   * @protected
   */
  protected getHours(config: calendarAdjustedConfig) {
    const totalHours = Math.ceil(
      (config.adjustedEndDate.getTime() - (config.adjustedStartDate.getTime())) / (1000 * 60 * 60)
    );
    return Array.from({length: totalHours}, (_, i) => i);
  }

  /**
   * Set whether the calendar is currently scrolling
   * @param scrolling
   * @protected
   */
  protected setScrolling(scrolling: boolean) {
    this._scrolling$.next(scrolling);
  }

  /**
   * Get the CSS height for a duration in minutes
   * @param durationMinutes
   * @protected
   */
  protected getMinuteHeight(durationMinutes: number) {
    return `${durationMinutes * this.minuteHeightRem}rem`;
  }

  /**
   * Get the Date object for a click event Y position based on the calendar config
   * @param container
   * @param event
   * @param config
   * @protected
   */
  protected getDateOfClickY(container: HTMLDivElement, event: MouseEvent, config: calendarAdjustedConfig) {
    const rect = container.getBoundingClientRect();
    const clickY = event.clientY - rect.top + container.scrollTop;
    const totalHeight = container.scrollHeight;
    const clickPercent = clickY / totalHeight;
    const clickTime = (config.adjustedEndDate.getTime() - config.adjustedStartDate.getTime()) * clickPercent;
    return new Date(config.adjustedStartDate.getTime() + clickTime);
  }

  /**
   * Get the Date object for a given hour index based on the calendar config
   * @param hourIndex
   * @param config
   * @protected
   */
  protected getDayOfHour(hourIndex: number, config: calendarAdjustedConfig) {
    return new Date(config.adjustedStartDate.getTime() + hourIndex * 60 * 60 * 1000);
  }

  /**
   * Generate a grid layout based on the calendar (layout) config,
   * containing locations and shift columns consistent across all days
   * @param config
   * @protected
   */
  protected getGridColumns(config: calendarAdjustedConfig){

    const createLocationColumns = (locationName: string, columnCount: number) =>
      `venue-${locationName}-start venue-${locationName}-activity-start] ${
        config.activityWidth ?? this.activityWidth
      } [venue-${locationName}-activity-end venue-${locationName}-shift-start ${
        [...Array(Math.max(1,columnCount) /* at least 1 column */ ).fill(undefined).map((_, index) =>
          `venue-${locationName}-shift-col-${index + 1}-start] ${
            this.shiftWidth
          } [venue-${locationName}-shift-col-${index + 1}-end`),
          config.shiftPaddingColumn ? `venue-${locationName}-shift-col-${columnCount}-start] ${
            config.activityWidth ?? this.activityWidth
          } [venue-${locationName}-shift-col-${Math.max(1,columnCount) + 1}-end` : ""
        ].join(" ")
      } venue-${locationName}-shift-end venue-${locationName}-end venue-${locationName}-gap-start] ${
        this.venueGapWidth
      } [venue-${locationName}-gap-end`;

    return `[time-start] ${this.dateWidth} [time-end ${
      (config.locationLayouts ?? []).sort((a,b) => {
        const aId = a.location.id.toLowerCase();
        const bId = b.location.id.toLowerCase();
        if(aId < bId) {
          return -1;
        }
        if(aId > bId) {
          return 1;
        }
        return 0;
      }).map(locationColumn => {
        const locationName = locationColumn.location.id;
        return createLocationColumns(
          locationName,
          Math.max(
            locationColumn.requiredShiftColumns,
            this.getLoadedShiftColumnCount(locationColumn.location.id)
          )
        );
      }).join(" ")
    } ${
      config.noLocationLayout === undefined ? "" : createLocationColumns(
        "none",
        Math.max(
          config.noLocationLayout.requiredShiftColumns,
          this.getLoadedShiftColumnCount()
        )
      )
    } blank-start] auto [blank-end filter-start]  ${this.dateWidth} [filter-end]`;
  }

  /**
   * Get the duration of a shift in minutes
   * @param shift
   * @protected
   */
  protected getItemMinutesDuration(shift: ShiftDto | ActivityDto){
      const start = new Date(shift.startTime);
      const end = new Date(shift.endTime);
      const durationMs = end.getTime() - start.getTime();
      return Math.floor(durationMs / 60000); // convert ms to minutes
  }

  /**
   * Get the minutes from the start of the calendar day to the shift start time
   * @param shift
   * @param config
   * @protected
   */
  protected getItemMinutesFromStart(shift: ShiftDto | ActivityDto, config: calendarAdjustedConfig) {
    const shiftStart = new Date(shift.startTime);
    return this.getDateMinutesFromStart(shiftStart, config);
  }

  /**
   * Get the minutes from the start of the calendar day to a given date
   * @param date
   * @param config
   * @protected
   */
  protected getDateMinutesFromStart(date: Date, config: calendarAdjustedConfig) {
    const durationMs = date.getTime() - config.adjustedStartDate.getTime();
    return Math.floor(durationMs / 60000); // convert ms to minutes
  }

  /**
   * Get the display/color category for a shift based on signup state
   * @param shift
   * @protected
   */
  protected getShiftDisplayCategory(shift: ShiftDto): "eligible" | "signed-up" | "" {
    if(shift.positionSlots.some(slot => slot.positionSignupState ===  PositionSlotDto.PositionSignupStateEnum.SignedUp)){
      return "signed-up";
    }
    if(shift.positionSlots.some(slot =>
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupViaAuction ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupViaTrade ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupPossible ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupOrTrade
    )){
      return "eligible";
    }

    return "";
  }

  /**
   * Get the caption/tag for a shift based on signup state
   * @param shift
   * @protected
   */
  protected getShiftDisplayTag(shift: ShiftDto): string {
    if(shift.positionSlots.some(slot => slot.positionSignupState ===  PositionSlotDto.PositionSignupStateEnum.SignedUp)){
      return "Signed Up";
    }
    if(shift.positionSlots.some(slot =>
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupViaAuction ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupViaTrade ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupPossible ||
      slot.positionSignupState === PositionSlotDto.PositionSignupStateEnum.SignupOrTrade
    )){
      return "Eligible";
    }

    return "";
  }

  private getScrolledDate(container: HTMLDivElement, config: calendarAdjustedConfig) {
    const scrollPercent = container.scrollTop / container.scrollHeight;
    const scrollTime = (config.adjustedEndDate.getTime() - config.adjustedStartDate.getTime()) * scrollPercent;
    return new Date(config.adjustedStartDate.getTime() + scrollTime);
  }

  /**
   * Get the currently visible days based on scroll position
   * DateTimes are in UTC, should represent 00:00:00 in user timezone.
   * Days that are only visible to display whole days for user timezone shifts,
   * but are outside of the UTC schedule bounds, are not included.
   * @param container
   * @param config
   * @private
   */
  private getVisibleValidDays(container: HTMLDivElement, config: calendarAdjustedConfig) {
    const scrollPercent = container.scrollTop / container.scrollHeight;
    const scrollTime = (config.adjustedEndDate.getTime() - config.adjustedStartDate.getTime()) * scrollPercent;
    const scrolledDate = config.adjustedStartDate.getTime() + scrollTime;

    const elementHeight = container.getBoundingClientRect().height;
    const visiblePercent = elementHeight / container.scrollHeight;
    const visibleTime = (config.adjustedEndDate.getTime() - config.adjustedStartDate.getTime()) * visiblePercent;
    const visibleHours = visibleTime / (1000 * 60 * 60);

    const dates = [new Date(scrolledDate).setUTCHours(0,0,0,0)];

    let addRemainderHours = visibleHours + (scrolledDate - dates[0]) / (1000 * 60 * 60);
    while(addRemainderHours > 24) {
      addRemainderHours -= 24;
      dates.push(dates[dates.length - 1] + 24 * 60 * 60 * 1000);
    }

    return dates
      .map(time => new Date(time))
      .filter(date => date.getTime() <= config.adjustedEndDate.getTime() && date.getTime() >= config.adjustedStartDate.getTime());
  }

  private getLoadedShiftColumnCount(locationId?: string) {
    const loadedDays = [...this.loadedDays$.getValue().values()];

    return loadedDays.reduce((maxCount, day) => {
      const matchingShiftColumns = locationId === undefined ?
        day.scheduleContentNoLocationDto?.shiftColumns ?? [] :
        day.scheduleContentDtos
          .filter(schedule => schedule.location.id === locationId)
          .flatMap(schedule => schedule.shiftColumns);

      return Math.max(maxCount, this.getRequiredShiftColumnCount(matchingShiftColumns));
    }, 0);
  }

  private getRequiredShiftColumnCount(shiftColumns: ShiftColumnDto[]) {
    return shiftColumns.reduce(
      (maxCount, shiftColumn) => Math.max(maxCount, shiftColumn.columnIndex + 1),
      0
    );
  }
}
