import {Component, inject} from "@angular/core";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {InputSliderComponent} from "../inputs/input-slider/input-slider.component";
import {TypedFormControlDirective} from "../../directives/typed-form-control.directive";
import {NgClass} from "@angular/common";

@Component({
  selector: "app-shift-preference",
  imports: [
    InputSliderComponent,
    TypedFormControlDirective,
    ReactiveFormsModule,
    NgClass
  ],
  standalone: true,
  templateUrl: "./shift-preference.component.html",
  styleUrl: "./shift-preference.component.scss"
})
export class ShiftPreferenceComponent {

  public readonly form;
  private readonly _fb = inject(FormBuilder);

  constructor() {

    this.form = this._fb.group({
      preference: this._fb.nonNullable.control<number>(0),
    });
  }

}
