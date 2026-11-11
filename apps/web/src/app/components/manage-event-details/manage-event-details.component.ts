import {Component, inject, Input, Output} from "@angular/core";
import {InputTextComponent} from "../inputs/input-text/input-text.component";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputDateComponent} from "../inputs/input-date/input-date.component";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {EventDto, EventEndpointService} from "../../../shiftservice-client";
import {mapValue} from "../../util/value-maps";
import {ActivatedRoute, Router} from "@angular/router";
import {BehaviorSubject, Subject} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {DialogComponent} from "../dialog/dialog.component";
import {icons} from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {captionLengthValidator, descriptionLengthValidator, nameLengthValidator} from "../../util/textValidators";

@Component({
  selector: "app-manage-event-details",
  imports: [
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    FaIconComponent,
    InputDateComponent,
    InputButtonComponent,
    AsyncPipe,
    DialogComponent
  ],
  templateUrl: "./manage-event-details.component.html",
  styleUrl: "./manage-event-details.component.scss"
})
export class ManageEventDetailsComponent {

  @Output()
  public readonly eventChanged = new Subject<void>();

  protected readonly form;
  protected readonly icons = icons;

  protected readonly manageData$ = new BehaviorSubject<{ event: undefined | EventDto } | undefined>(undefined);

  protected showEventDeleteConfirm = false;

  private readonly _fb = inject(FormBuilder);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _toastService = inject(ToastService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);

  constructor() {
    this.form = this._fb.group({
      name: this._fb.nonNullable.control<string>("", [nameLengthValidator, Validators.required]),
      shortDescription: this._fb.nonNullable.control<string>("", [captionLengthValidator]),
      longDescription: this._fb.nonNullable.control<string>("", [descriptionLengthValidator]),
      startDate: this._fb.nonNullable.control<Date>(new Date()),
      endDate: this._fb.nonNullable.control<Date>(new Date()),
      socials: this._fb.nonNullable.control<string>(""),
      rewardsUrl: this._fb.nonNullable.control<string>("")
    });
  }

  @Input()
  public set event(value: { event: EventDto | undefined }) {
    this.manageData$.next(value);

    if(value?.event !== undefined) {
      this.form.setValue({
        name: value.event.name,
        shortDescription: value.event.shortDescription ?? "",
        longDescription: value.event.longDescription ?? "",
        startDate: new Date(value.event.startTime),
        endDate: new Date(value.event.endTime),
        socials: value.event.socialMediaLinks.map(social => social.url).join(", "),
        rewardsUrl: value.event.rewardPointsRedeemUrl ?? ""
      });
    } else {
      this.form.reset();
    }
  }

  protected create(){

    this.form.markAllAsTouched();

    if(this.form.valid) {
      this._eventService.createEvent({
        name: this.form.controls.name.value,
        shortDescription: mapValue.undefinedIfEmptyString(this.form.controls.shortDescription.value),
        longDescription: mapValue.undefinedIfEmptyString(this.form.controls.longDescription.value),
        startTime: mapValue.dateAsLocalDateStartOfDayString(this.form.controls.startDate.value),
        endTime: mapValue.dateAsLocalDateEndOfDayString(this.form.controls.endDate.value),
        socialLinks: mapValue.undefinedIfEmptyString(this.form.controls.socials.value),
        rewardPointsRedeemUrl: mapValue.undefinedIfEmptyString(this.form.controls.rewardsUrl.value)
      }).pipe(
        this._toastService.tapCreating("Event", event => event.name)
      ).subscribe(created => this._router.navigate([`../${created.id}/manage`], {relativeTo: this._route}));
    } else {
      this._toastService.showError("Invalid Event", "Please provide valid event details.");
    }
  }

  /**
   * let user choose a xlsx file, and upload event data from it
   * @protected
   */
  protected import() {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".xlsx";
    input.onchange = () => {
      if (input.files && input.files.length > 0) {
        const file = input.files[0];
        this._eventService.importEventData(file).pipe(
          this._toastService.tapSuccess("Imported Event Data", event => `Event data imported successfully for event ${event.event.name}.`),
          this._toastService.tapError("Import Failed", mapValue.apiErrorToMessage)
        ).subscribe(created => this._router.navigate([`../${created.event.id}/manage`], {relativeTo: this._route}));
      }
    };
    input.click();
  }

  protected downloadTemplate() {
    this._eventService.downloadEventImportTemplate().subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "event_import_template.xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    });
  }

  protected update(event: EventDto){
    this.form.markAllAsTouched();

    if(this.form.valid) {
      this._eventService.updateEvent(event.id, {
        name: this.form.controls.name.value,
        shortDescription: mapValue.undefinedIfEmptyString(this.form.controls.shortDescription.value),
        longDescription: mapValue.undefinedIfEmptyString(this.form.controls.longDescription.value),
        startTime: mapValue.dateAsLocalDateStartOfDayString(this.form.controls.startDate.value),
        endTime: mapValue.dateAsLocalDateEndOfDayString(this.form.controls.endDate.value),
        socialLinks: mapValue.undefinedIfEmptyString(this.form.controls.socials.value),
        rewardPointsRedeemUrl: mapValue.undefinedIfEmptyString(this.form.controls.rewardsUrl.value)
      }).pipe(
        this._toastService.tapSaving("Event", updated => updated.name)
      ).subscribe(() => this.eventChanged.next());
    } else {
      this._toastService.showError("Invalid Event", "Please provide valid event details.");
    }
  }

  protected delete(event: EventDto) {
    this._eventService.deleteEvent(event.id).pipe(
      this._toastService.tapDeleting("Event")
    ).subscribe(() => this._router.navigate(["../../"], {relativeTo: this._route}));
  }
}
