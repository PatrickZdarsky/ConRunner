import {Component, inject, Input, OnDestroy} from "@angular/core";
import {
  PaginationDtoUserPlanDto,
  RoleDto,
  ShiftPlanDto,
  UserEventEndpointService,
  UserPlanEndpointService,
  VolunteerDto
} from "../../../shiftservice-client";
import {
  BehaviorSubject, catchError,
  combineLatest,
  combineLatestWith,
  debounceTime, distinctUntilChanged,
  EMPTY,
  filter,
  map, Observable, of, pairwise,
  shareReplay,
  startWith,
  switchMap
} from "rxjs";
import { icons } from "../../util/icons";
import {FormBuilder, FormControl, ReactiveFormsModule} from "@angular/forms";
import {AsyncPipe} from "@angular/common";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {ToastService} from "../../services/toast/toast.service";
import {InputMultiselectComponent} from "../inputs/input-multiselect/input-multiselect.component";
import {InputToggleComponent} from "../inputs/input-toggle/input-toggle.component";
import {MinPipe} from "../../pipes/min.pipe";

export interface planVolunteersData {
  plan: ShiftPlanDto;
  roles: RoleDto[];
}

@Component({
  selector: "app-manage-plan-volunteers",
  imports: [
    AsyncPipe,
    InputButtonComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputMultiselectComponent,
    InputToggleComponent,
    MinPipe
  ],
  templateUrl: "./manage-plan-volunteers.component.html",
  styleUrl: "./manage-plan-volunteers.component.scss"
})
export class ManagePlanVolunteersComponent implements OnDestroy {

  protected page$: Observable<PaginationDtoUserPlanDto>;
  protected readonly manageData$ = new BehaviorSubject<planVolunteersData | undefined>(undefined);
  protected readonly roleSelectOptions$ = this.manageData$.pipe(
    filter((data): data is planVolunteersData => data !== undefined),
    map(data => data.roles
      .sort((a,b) => a.name.localeCompare(b.name))
      .map(role => ({
        name: role.name,
        value: role
    })))
  );
  protected readonly roleSelectControls$;
  protected readonly lockControls$;
  protected readonly icons = icons;
  protected readonly form;
  protected readonly pageSize = 20;

  private readonly _fb = inject(FormBuilder);
  private readonly _toastService = inject(ToastService);
  private readonly _userPlanService = inject(UserPlanEndpointService);
  private readonly _userEventService = inject(UserEventEndpointService);
  private readonly _roleChangesSubscription;
  private readonly _lockChangesSubscription;

  constructor() {
    this.form = this._fb.group({
      paginationIndex: this._fb.nonNullable.control<number>(0),
      search: this._fb.nonNullable.control<string>("")
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
        filter((data): data is planVolunteersData => data !== undefined
      ))),
      switchMap(([value, manageData]) => this._userPlanService.getAllUsersOfPlan(
        manageData.plan.id,
        value.paginationIndex ?? 0,
        this.pageSize,
        value.search,
        "body")
      ),
      shareReplay()
    );

    this.roleSelectControls$ = this.page$.pipe(
      combineLatestWith(this.roleSelectOptions$, this.manageData$.pipe(
        filter((data): data is planVolunteersData => data !== undefined
      ))),
      map(([page, options, data]) => {
        const controls = new Map<string, FormControl<RoleDto[]>>();

        page.items.forEach(v => {
          const control = this._fb.nonNullable.control<RoleDto[]>([]);
          control.setValue(v.roles);
          control.markAsUntouched();

          controls.set(v.volunteer.id, control);
        });

        return {controls, options, data};
      }),
      shareReplay()
    );

    this.lockControls$ = this.page$.pipe(
      combineLatestWith(this.manageData$.pipe(
        filter((data): data is planVolunteersData => data !== undefined
      ))),
      map(([page, data]) => {
        const controls = new Map<string, FormControl<boolean>>();

        page.items.forEach(v => {
          const control = this._fb.nonNullable.control<boolean>(false);
          control.setValue(v.isLocked);
          control.markAsUntouched();

          controls.set(v.volunteer.id, control);
        });

        return {controls, data};
      }),
      shareReplay()
    );

    this._roleChangesSubscription = this.roleSelectControls$.pipe(
      switchMap(({controls, data}) => {
        const changes = Array.from(controls.entries()).map(([userId, control]) =>
          control.valueChanges.pipe(
            filter(() => control.touched),
            distinctUntilChanged(),
            debounceTime(100),
            switchMap(roles => this._userPlanService.updateUserRoles(data.plan.id, userId, {
              roles: roles.map(r => r.id)
            }).pipe(
              this._toastService.tapSaving("Volunteer Roles"),
              catchError(() => of(undefined))
            )),
          )
        );
        return combineLatest(changes);
      })
    ).subscribe();

    this._lockChangesSubscription = this.lockControls$.pipe(
      switchMap(({controls, data}) => {
        const changes = Array.from(controls.entries()).map(([userId, control]) =>
          control.valueChanges.pipe(
            filter(() => control.touched),
            distinctUntilChanged(),
            debounceTime(100),
            switchMap(isLocked => (isLocked ? this._userEventService.lockUserInPlan(userId, {
              shiftPlanIds: [data.plan.id]
            }) : this._userEventService.unlockUserInPlan(userId, {
              shiftPlanIds: [data.plan.id]
            })).pipe(
              (isLocked ? this._toastService.tapSaving("Access Lock") : this._toastService.tapDeleting("Access Lock")),
              catchError(() => of(undefined))
            )),
          )
        );
        return combineLatest(changes);
      })
    ).subscribe();
  }

  @Input()
  public set manageData(data: planVolunteersData | undefined) {
    this.manageData$.next(data);
    this.form.patchValue({paginationIndex: 0, search: ""});
  }

  ngOnDestroy(): void {
    this._roleChangesSubscription.unsubscribe();
    this._lockChangesSubscription.unsubscribe();
  }

  protected idComparator(a: RoleDto | null, b: RoleDto | null) {
    return a?.id === b?.id;
  }

  protected removeFromPlan(volunteer: VolunteerDto, plan: ShiftPlanDto){
    this._userEventService.bulkRemoveVolunteeringPlans({
      volunteers: [volunteer.id],
      plans: [plan.id]
    }).pipe(
      this._toastService.tapDeleting("Plan Access")
    ).subscribe(() => this.form.patchValue(this.form.value));
  }

  protected resetInPlan(volunteer: VolunteerDto, plan: ShiftPlanDto){
    this._userEventService.resetUserInPlan(volunteer.id, {
      shiftPlanIds: [plan.id]
    }).pipe(
      this._toastService.tapDeleting("Volunteer Assignments")
    ).subscribe(() => this.form.patchValue(this.form.value));
  }

}
