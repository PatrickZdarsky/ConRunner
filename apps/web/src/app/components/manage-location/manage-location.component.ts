import {Component, inject, Input, Output} from "@angular/core";
import {
  EventDto,
  LocationDto, LocationEndpointService,
  LocationModificationDto
} from "../../../shiftservice-client";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {mapValue} from "../../util/value-maps";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {AsyncPipe, NgClass} from "@angular/common";
import {DialogComponent} from "../dialog/dialog.component";
import {icons} from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {BehaviorSubject, map, Subject} from "rxjs";
import {descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";

@Component({
  selector: "app-manage-location",
  imports: [
    FaIconComponent,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent,
    NgClass,
    DialogComponent,
    AsyncPipe
  ],
  templateUrl: "./manage-location.component.html",
  styleUrl: "./manage-location.component.scss"
})
export class ManageLocationComponent {

  @Output()
  public locationChanged = new Subject<void>();

  protected readonly icons = icons;
  protected readonly form;
  protected readonly manageData$ =
    new BehaviorSubject<undefined | { location: LocationDto | undefined; event: EventDto }>(undefined);
  protected readonly isPretalxManaged$ =this.manageData$.pipe(
    map(data => data?.location?.readOnly ?? false)
  );

  protected showDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _locationService = inject(LocationEndpointService);
  private readonly _toastService = inject(ToastService);

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [Validators.required, nameLengthValidator]),
      description: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      url: this._fb.nonNullable.control<string>("")
    });
  }

  @Input()
  public set manageData(value: { location: LocationDto | undefined; event: EventDto } | undefined) {
    this.manageData$.next(value);

    if(value !== undefined) {
      this.form.setValue({
        name: value.location?.name ?? "",
        description: value.location?.description ?? "",
        url: value.location?.url ?? ""
      });
    } else {
      this.form.reset();
    }
    this.form.markAsPristine();
  }

  protected save(location: LocationDto | undefined, event: EventDto) {
    this.form.markAllAsTouched();

    if(this.form.invalid) {
      this._toastService.showError("Invalid Location", "Please provide valid location details.");
      return;
    }

    const locationData: LocationModificationDto = {
      name: this.form.controls.name.value,
      description: mapValue.undefinedIfEmptyString(this.form.controls.description.value),
      url: mapValue.undefinedIfEmptyString(this.form.controls.url.value),
    };

    (location === undefined ?
      this._locationService.createLocation(event.id, locationData) :
      this._locationService.updateLocation(location.id, locationData)
    ).pipe(
      location === undefined ?
        this._toastService.tapCreating("Location", item => item.name) :
        this._toastService.tapSaving("Location", item => item.name)
    ).subscribe(() => {
      this.locationChanged.next();

      if(location === undefined) {
        this.form.reset();
      }
    });
  }

  protected delete(location: LocationDto) {
    this._locationService.deleteLocation(location.id).pipe(
      this._toastService.tapDeleting("Location")
    ).subscribe(() => {
      this.locationChanged.next();
    });
  }

  protected getOrder(location: LocationDto | undefined) {
    const order = -1 * Number(location?.id);
    if(isNaN(order)) {
      return Number.MIN_SAFE_INTEGER;
    }
    return order;
  }
}
