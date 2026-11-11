import {Component, inject} from "@angular/core";
import {PageService} from "../../../services/page/page.service";
import {BC_SHIFT_DETAILS, BC_EVENT} from "../../../breadcrumbs";
import {ActivatedRoute, Router} from "@angular/router";
import {ShiftDetailsDto, ShiftEndpointService} from "../../../../shiftservice-client";
import {BehaviorSubject, filter, map, Observable} from "rxjs";
import {ManageShiftComponent, manageShiftParams} from "../../../components/manage-shift/manage-shift.component";
import {AsyncPipe} from "@angular/common";

@Component({
  selector: "app-shift-details",
  imports: [ManageShiftComponent, AsyncPipe],
  standalone: true,
  templateUrl: "./shift-details.component.html",
  styleUrl: "./shift-details.component.scss"
})
export class ShiftDetailsComponent {

  protected shift$ = new BehaviorSubject<ShiftDetailsDto | undefined>(undefined);
  protected manageShiftParams$: Observable<manageShiftParams>;

  private readonly _pageService = inject(PageService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _shiftService = inject(ShiftEndpointService);
  private readonly _shiftId;

  constructor() {
    const shiftId = this._route.snapshot.paramMap.get("shiftId");
    if(shiftId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Shift ID is required");
    }
    this._shiftId = shiftId;

    this._shiftService.getShiftDetails(shiftId).subscribe(shift => this.shift$.next(shift));

    this.manageShiftParams$ = this.shift$.pipe(
      filter(shift => shift !== undefined),
      map(shift => ({
        planId: shift.shiftPlan.id,
        eventId: shift.event.id,
        shift: shift.shift,
        availableActivities: shift.shift.relatedActivity === undefined ?
          [] :
          [{name: shift.shift.relatedActivity.name, value: shift.shift.relatedActivity}],
        availableRoles: shift.shift.positionSlots
          .map(slot => slot.role)
          .filter(role => role !== undefined)
          .map(role => ({name: role.name, value: role})),
        availableLocations: shift.shift.location === undefined ?
          [] :
          [{name: shift.shift.location.name, value: shift.shift.location}]
      }))
    );

    this.shift$.pipe(
      filter(shift => shift !== undefined)
    ).subscribe(shift => {
      this._pageService
        .configurePageName(shift.shift.name)
        .configureBreadcrumb(BC_EVENT, shift.event.name, shift.event.id)
        .withCalendarLink("/events/" + shift.event.id + "/calendar")
        .configureBreadcrumb(BC_SHIFT_DETAILS, shift.shift.name, shift.shift.id);
    });
  }

  public refetchShift() {
    this._shiftService.getShiftDetails(this._shiftId).subscribe(shift => this.shift$.next(shift));
  }

  public goToPlanDashboard(planId?: string) {
    if(planId === undefined) {
      throw new Error("PlanId is required");
    }
    this._router.navigateByUrl(`/plans/${planId}`);
  }

}
