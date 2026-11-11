import {Component, inject, Input, Output} from "@angular/core";
import {BehaviorSubject, combineLatestWith, filter, map, shareReplay, Subject} from "rxjs";
import {ShiftPlanEndpointService, UserEventDto} from "../../../shiftservice-client";
import {icons} from "../../util/icons";
import {FormBuilder} from "@angular/forms";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {AsyncPipe} from "@angular/common";
import {ManageVolunteerPlanComponent} from "./manage-volunteer-plan/manage-volunteer-plan.component";

@Component({
  selector: "app-manage-volunteer",
  imports: [
    FaIconComponent,
    AsyncPipe,
    ManageVolunteerPlanComponent
  ],
  templateUrl: "./manage-volunteer.component.html",
  styleUrl: "./manage-volunteer.component.scss"
})
export class ManageVolunteerComponent {

  @Output()
  public volunteerChanged = new Subject<UserEventDto>();

  protected readonly icons = icons;
  protected readonly form;
  protected readonly volunteer$ = new BehaviorSubject<UserEventDto | undefined>(undefined);
  protected readonly plans$;
  protected readonly newPlanData$;
  protected readonly volunteerPlanRows$;

  private readonly _fb = inject(FormBuilder);
  private readonly planService = inject(ShiftPlanEndpointService);

  constructor() {
    this.form = this._fb.group({
      paginationIndex: this._fb.nonNullable.control<number>(0)
    });

    this.plans$ = this.planService.getAllShiftPlans().pipe(
      shareReplay()
    );

    this.volunteerPlanRows$ = this.volunteer$.pipe(
      filter((volunteer): volunteer is UserEventDto => volunteer !== undefined),
      combineLatestWith(this.plans$),
      map(([volunteer, plans]) => {
        const relevantPlanIds = new Set<string>();
        volunteer.volunteeringPlans.forEach(id => relevantPlanIds.add(id));
        volunteer.planningPlans.forEach(id => relevantPlanIds.add(id));
        volunteer.lockedPlans.forEach(id => relevantPlanIds.add(id));
        return [volunteer, plans.filter(plan => relevantPlanIds.has(plan.id))] as const;
      }),
      map(([volunteer, plans]) => plans.map(plan => ({
          volunteer,
          plans,
          plan
        })))
    );

    this.newPlanData$ = this.volunteer$.pipe(
      filter((volunteer): volunteer is UserEventDto => volunteer !== undefined),
      combineLatestWith(this.plans$),
      map(([volunteer, plans]) => {
        const relevantPlanIds = new Set<string>();
        volunteer.volunteeringPlans.forEach(id => relevantPlanIds.add(id));
        volunteer.planningPlans.forEach(id => relevantPlanIds.add(id));
        volunteer.lockedPlans.forEach(id => relevantPlanIds.add(id));
        return [volunteer, plans.filter(plan => !relevantPlanIds.has(plan.id))] as const;
      }),
      map(([volunteer, plans]) => ({
        volunteer,
        plans,
        plan: undefined
      }))
    );
  }

  @Input()
  public set volunteer(volunteer: UserEventDto) {
    this.volunteer$.next(volunteer);
  }
}
