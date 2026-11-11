import {Component, EventEmitter, inject, Input, Output} from "@angular/core";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {DialogComponent, dialogResult} from "../dialog/dialog.component";
import {InputDateComponent} from "../inputs/input-date/input-date.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {mapValue} from "../../util/value-maps";

@Component({
  selector: "app-dialog-add-emergency",
  imports: [
    DialogComponent,
    InputDateComponent,
    TypedFormControlDirective,
    ReactiveFormsModule
  ],
  standalone: true,
  templateUrl: "./dialog-add-emergency.component.html",
  styleUrl: "./dialog-add-emergency.component.scss"
})
export class DialogAddEmergencyComponent {

  /**
   * event when the dialog should be closed
   */
  @Output()
  public readonly result = new EventEmitter<Date | undefined>();

  @Input({required: true})
  public viewWhen?: boolean;

  public readonly form;
  private readonly _fb = inject(FormBuilder);

  constructor(){
    this.form = this._fb.group({
      emergencyDate: this._fb.nonNullable.control<Date>(new Date())
    });
  }

  closed(result: dialogResult) {
    if (result === "success") {
      const date = this.form.controls.emergencyDate.value;
      const dayBegin = mapValue.localDayBeginFromDatetimeString(date.toDateString());
      this.result.emit(dayBegin);
      this.form.reset();
    } else {
      this.result.emit(undefined);
    }
  }
}
