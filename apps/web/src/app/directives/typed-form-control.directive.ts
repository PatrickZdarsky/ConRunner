/* eslint-disable @angular-eslint/directive-selector */
/* eslint-disable @angular-eslint/no-input-rename */
import { Directive, Input } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import {TypedControlValueAccessor} from "../util/typedControlValueAccessor";

/**
 * a directive that does nothing else but infer types of a formControl directive binding to a FormControl object
 * can be used to statically fixate types of a form binding; exposes minimal overhead as atradeoff
 *
 * @example <app-ngf-input-select #selection [xsb-typed]="[selection, form.controls.option]" [formControl]="form.controls.option" />
 */
@Directive({
  standalone: true,
  selector: "[xsb-typed]"
})
export class TypedFormControlDirective<TData> {

  /**
   * Input to statically check the type compatibility between form component and control binding
   */
  @Input({ required: true, alias: "xsb-typed" })
  typeValidation!: [TypedControlValueAccessor<TData>, AbstractControl<TData>];
}

