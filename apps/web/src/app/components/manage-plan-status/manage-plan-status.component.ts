import {Component, inject, Input} from "@angular/core";
import {
  AssignmentDto,
  PaginationDtoShiftDto,
  ShiftPlanDto, SignupEndpointService
} from "../../../shiftservice-client";
import {
  BehaviorSubject,
  combineLatestWith,
  debounceTime,
  EMPTY,
  filter, map,
  Observable, of, pairwise,
  shareReplay,
  startWith,
  switchMap
} from "rxjs";
import { icons } from "../../util/icons";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {AsyncPipe, NgClass} from "@angular/common";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {MinPipe} from "../../pipes/min.pipe";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {RouterLink} from "@angular/router";

export interface planStatusData {
  plan: ShiftPlanDto;
}

@Component({
  selector: "app-manage-plan-status",
  imports: [
    AsyncPipe,
    InputButtonComponent,
    ReactiveFormsModule,
    MinPipe,
    FaIconComponent,
    NgClass,
    RouterLink
  ],
  templateUrl: "./manage-plan-status.component.html",
  styleUrl: "./manage-plan-status.component.scss"
})
export class ManagePlanStatusComponent {

  protected page$: Observable<PaginationDtoShiftDto>;
  protected processedPage$;
  protected readonly manageData$ = new BehaviorSubject<planStatusData | undefined>(undefined);
  protected readonly icons = icons;
  protected readonly form;
  protected readonly pageSize = 20;

  private readonly _fb = inject(FormBuilder);
  private readonly _signupService = inject(SignupEndpointService);

  constructor() {
    this.form = this._fb.group({
      paginationIndex: this._fb.nonNullable.control<number>(0)
    });

    this.page$ = this.form.valueChanges.pipe(
      startWith(this.form.value),
      pairwise(),
      switchMap(([previousValue, value]) => {

        /* something else than page index changed, reset page */
        if(value.paginationIndex === previousValue.paginationIndex && value.paginationIndex !== 0) {
          this.form.controls.paginationIndex.setValue(0, {emitEvent: true});
          return EMPTY;
        }

        /* page index changed, keep filters */
        return of(value);
      }),
      startWith(this.form.value),
      debounceTime(100),
      combineLatestWith(this.manageData$.pipe(
        filter((data): data is planStatusData => data !== undefined
      ))),
      switchMap(([value, manageData]) => this._signupService.getAllOpenShifts(
        manageData.plan.id,
        value.paginationIndex ?? 0,
        this.pageSize)
      ),
      shareReplay()
    );

    this.processedPage$ = this.page$.pipe(
      map(page => ({
          ...page,
          items: page.items.map(shift => ({
            ...shift,
            openSlotsCount: shift.positionSlots
              .filter(slot => slot
                .assignments
                .filter(ass => this.assignmentIsSignedUp(ass))
              )
              .length,
            slotCount: shift.positionSlots.length,
            openAssignmentsCount: shift.positionSlots
              .flatMap(slot => slot
                .assignments
                .filter(ass => this.assignmentIsSignedUp(ass))
              )
              .length,
            requiredCapacityCount: shift.positionSlots
              .map(slot => slot.desiredVolunteerCount)
              .reduce((a, b) => a + b)
          }))
        })),
      shareReplay()
    );
  }

  @Input()
  public set manageData(data: planStatusData | undefined) {
    this.manageData$.next(data);
    this.form.patchValue({paginationIndex: 0});
  }

  protected assignmentIsSignedUp(assignment: AssignmentDto){
    return assignment.status !== AssignmentDto.StatusEnum.RequestForAssignment;
  }

}
