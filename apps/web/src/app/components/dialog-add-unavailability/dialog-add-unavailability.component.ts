import {Component, effect, EventEmitter, inject, Input, Output} from "@angular/core";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {DialogComponent, dialogResult} from "../dialog/dialog.component";
import {InputToggleComponent} from "../inputs/input-toggle/input-toggle.component";
import {InputDateComponent} from "../inputs/input-date/input-date.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {toSignal} from "@angular/core/rxjs-interop";
import {InputTimeComponent, time} from "../inputs/input-time/input-time.component";
import {mapValue} from "../../util/value-maps";

export interface addUnavailabilityInput {
  start?: Date;
  end?: Date;
}

@Component({
  selector: "app-dialog-add-unavailability",
  imports: [
    DialogComponent,
    InputToggleComponent,
    InputDateComponent,
    InputTimeComponent,
    TypedFormControlDirective,
    ReactiveFormsModule
  ],
  standalone: true,
  templateUrl: "./dialog-add-unavailability.component.html",
  styleUrl: "./dialog-add-unavailability.component.scss"
})
export class DialogAddUnavailabilityComponent {

  /**
   * event when the dialog should be closed
   */
  @Output()
  public readonly result = new EventEmitter<addUnavailabilityInput | undefined>();

  @Input({required: true})
  public viewWhen?: boolean;

  public readonly form;
  private readonly _fb = inject(FormBuilder);

  constructor(){
    this.form = this._fb.group({
      unavailabilityFromEventStart: this._fb.nonNullable.control<boolean>(false),
      unavailabilityUntilEventEnd: this._fb.nonNullable.control<boolean>(false),
      unavailabilityFrom: this._fb.nonNullable.control<Date>(new Date()),
      unavailabilityUntil: this._fb.nonNullable.control<Date>(new Date()),
      unavailabilityFromTime: this._fb.nonNullable.control<time>({hour: 0, minute: 0}),
      unavailabilityUntilTime: this._fb.nonNullable.control<time>({hour: 23, minute: 59}),
    });

    /* react to toggles */
    const formSignal = toSignal(this.form.valueChanges);
    effect(() => {
      const form = formSignal();

      if(form?.unavailabilityFromEventStart === true) {
        if(this.form.controls.unavailabilityFrom.enabled) {
          this.form.controls.unavailabilityFrom.disable();
        }
        if(this.form.controls.unavailabilityFromTime.enabled) {
          this.form.controls.unavailabilityFromTime.disable();
        }
      } else {
        if(this.form.controls.unavailabilityFrom.disabled) {
          this.form.controls.unavailabilityFrom.enable();
        }
        if(this.form.controls.unavailabilityFromTime.disabled) {
          this.form.controls.unavailabilityFromTime.enable();
        }
      }

      if(form?.unavailabilityUntilEventEnd === true) {
        if(this.form.controls.unavailabilityUntil.enabled) {
          this.form.controls.unavailabilityUntil.disable();
        }
        if(this.form.controls.unavailabilityUntilTime.enabled) {
          this.form.controls.unavailabilityUntilTime.disable();
        }
      } else {
        if(this.form.controls.unavailabilityUntil.disabled) {
          this.form.controls.unavailabilityUntil.enable();
        }
        if(this.form.controls.unavailabilityUntilTime.disabled) {
          this.form.controls.unavailabilityUntilTime.enable();
        }
      }
    });
  }

  closed(result: dialogResult) {
    if (result === "success") {
      const values = this.form.value;
      const datetimeFrom = mapValue.combineDateAndLocalTime(values.unavailabilityFrom, values.unavailabilityFromTime);
      const datetimeUntil = mapValue.combineDateAndLocalTime(values.unavailabilityUntil, values.unavailabilityUntilTime);

      /* disabled controls are automatically undefined */
      this.result.emit({
        start: datetimeFrom,
        end: datetimeUntil
      });

      this.form.reset();
    } else {
      this.result.emit(undefined);
    }
  }
}
