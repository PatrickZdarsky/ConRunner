import {Component, EventEmitter, inject, Input, Output} from "@angular/core";
import {
  AssignmentDto,
  PositionSlotDto,
  PositionSlotEndpointService,
  SignupEndpointService,
  VolunteerDto
} from "../../../../shiftservice-client";
import {BehaviorSubject, debounceTime, map, Observable, of, shareReplay, startWith, switchMap, take} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {DialogComponent} from "../../dialog/dialog.component";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {InputMultiToggleComponent, MultiToggleOptions} from "../../inputs/input-multitoggle/input-multi-toggle.component";
import {icons} from "../../../util/icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../../inputs/input-button/input-button.component";
import {ToastService} from "../../../services/toast/toast.service";
import {mapValue} from "../../../util/value-maps";

export interface manageAssignmentsParams {
  position: PositionSlotDto;
}

type volunteerCandidates = Array<{
  volunteer: VolunteerDto;
  assignment: AssignmentDto | undefined;
}>;

@Component({
  selector: "app-dialog-manage-assignments",
  imports: [
    AsyncPipe,
    DialogComponent,
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputMultiToggleComponent,
    InputButtonComponent
  ],
  templateUrl: "./dialog-manage-assignments.component.html",
  styleUrl: "./dialog-manage-assignments.component.scss"
})
export class DialogManageAssignmentsComponent {

  @Output()
  public assignmentChanged = new EventEmitter<boolean>();

  protected readonly icons = icons;
  protected readonly form;
  protected readonly filterOptions: MultiToggleOptions<"assigned" | "unassigned" | "all"> = [
    {name: "Assigned", value: "assigned"},
    {name: "Unassigned", value: "unassigned"},
    {name: "All", value: "all"}
  ];

  protected hasChanges = false;

  protected readonly manageAssignmentsParams$ = new BehaviorSubject<manageAssignmentsParams | undefined>(undefined);
  protected readonly volunteers$: Observable<volunteerCandidates> = this.manageAssignmentsParams$.pipe(
    switchMap(params => params === undefined ?
      of([]) :
      this._signupService.getAssignableUsers("TODO REMOVE UNNECESSARY PARAM", params.position.id).pipe(
        map(volunteers => ([
          ...volunteers.map(volunteer => ({volunteer, assignment: undefined})),
          ...params.position.assignments.map(assignment => ({volunteer: assignment.assignedVolunteer, assignment}))
        ]))
      )),
    shareReplay()
  );
  protected readonly filteredVolunteers$: Observable<volunteerCandidates>;

  private readonly _signupService = inject(SignupEndpointService);
  private readonly _positionService = inject(PositionSlotEndpointService);
  private readonly _toastService = inject(ToastService);
  private readonly _fb = inject(FormBuilder);

  constructor() {
    this.form = this._fb.group({
      filterName: this._fb.nonNullable.control<string>(""),
      filterType: this._fb.nonNullable.control<"assigned" | "unassigned" | "all">("all")
    });

    this.filteredVolunteers$ = this.form.valueChanges.pipe(
      startWith(this.form.value),
      debounceTime(100),
      switchMap(filters => this.volunteers$.pipe(
        map(volunteers => volunteers.filter(vc => {
          const matchesName = filters.filterName === undefined || this.nameMatches(vc.volunteer, filters.filterName);
          const matchesType = filters.filterType === "all" ||
            (filters.filterType === "assigned" && vc.assignment !== undefined) ||
            (filters.filterType === "unassigned" && vc.assignment === undefined);
          return matchesName && matchesType;
        }))
      ))
    );
  }

  @Input()
  public set params(value: manageAssignmentsParams | undefined) {
    this.manageAssignmentsParams$.next(value);
  }

  /**
   * Assigns a volunteer to the position slot
   * @param volunteer
   * @param position
   * @protected
   */
  protected assignVolunteer(volunteer: VolunteerDto, position: PositionSlotDto): void {
    this._signupService.assignUsersToSlot("TODO REMOVE UNNECESSARY PARAM", {
      positionSlotId: position.id,
      volunteerIds: [volunteer.id]
    }).pipe(
      this._toastService.tapSuccess("Volunteer Assigned Successfully"),
      this._toastService.tapError("Volunteer Assignment Failed", mapValue.apiErrorToMessage),
    ).subscribe(() => {
      this.hasChanges = true;
      this.refetchPositionSlot();
    });
  }

  /**
   * Unassigns a volunteer from the position slot
   * @param assignment
   * @protected
   */
  protected unassignVolunteer(assignment: AssignmentDto): void {
    this._signupService.unAssignUsersFromSlot("TODO REMOVE UNNECESSARY PARAM", {
      volunteerIds: [assignment.assignedVolunteer.id],
      positionSlotId: assignment.positionSlotId
    }).pipe(
      this._toastService.tapSuccess("Volunteer Unassigned Successfully"),
      this._toastService.tapError("Volunteer Unassignment Failed", mapValue.apiErrorToMessage),
    ).subscribe(() => {
      this.hasChanges = true;
      this.refetchPositionSlot();
    });
  }

  /**
   * Refetches the position slot data to get refreshed volunteer infos
   * @private
   */
  private refetchPositionSlot() {
    this.manageAssignmentsParams$.pipe(
      take(1),
      switchMap(params => {
        if(params === undefined) {
          return of(undefined);
        }
        return this._positionService.getPositionSlot(params.position.id);
      })
    ).subscribe(position => this.manageAssignmentsParams$.next(position === undefined ? undefined :{position}));
  }

  private nameMatches(volunteer: VolunteerDto, nameFilter: string): boolean {
    const fullName = `${volunteer.firstName} ${volunteer.lastName}`.toLowerCase().replaceAll(" ", "");
    return fullName.includes(nameFilter.replaceAll(" ","").toLowerCase());
  }

}
