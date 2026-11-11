import {Component, HostBinding, inject, Injector, Input, OnInit} from "@angular/core";
import {FormsModule, NG_VALUE_ACCESSOR, NgControl} from "@angular/forms";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";

@Component({
  selector: "xsb-input-slider",
  templateUrl: "./input-slider.component.html",
  styleUrls: ["./input-slider.component.scss"],
  standalone: true,
  imports: [
    FormsModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputSliderComponent
    }
  ]
})
export class InputSliderComponent implements TypedControlValueAccessor<number>, OnInit {

  /**
   * the native html for attribute for accessibility
   */
  @Input()
  name = "";

  /**
   * step size of the slider
   */
  @Input()
  step = 1;

  /**
   * max value of the slider
   */
  @Input({required:true})
  max = 1;

  /**
   * min value of the slider
   */
  @Input({required:true})
  min = 1;

  /**
   * disguise the input as plain text and make immutable
   */
  @Input()
  disguise = false;

  /**
   * HTML placeholder of the input element
   */
  @Input()
  placeholder = "";

  /**
   * prohibit ID attribute on input
   */
  @Input()
  id = null;

  /** current disabled state */
  disabled = false;

  /** bound ngmodel to the underlying input */
  value = 0;

  /** inchange callback to be set by the parent form */
  onChange?: (value: number) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  private injector = inject(Injector);

  /* hide actual properties from html to prevent accessibility isues */
  @HostBinding("attr.name") get hideNameAttr() { return null; }
  @HostBinding("attr.id") get hideIdAttr() { return null; }
  @HostBinding("attr.value") get hideValueAttr() { return null; }
  @HostBinding("attr.placeholder") get hidePlaceholderAttr() { return null; }

  /**
   * current validity state determined by validators
   */
  get valid() {
    return this.ngControl ? !this.ngControl.invalid : true;
  }

  /**
   * get the form control (self) avoiding circular deps
   */
  ngOnInit(): void {
    this.ngControl = this.injector.get(NgControl);
  }

  /**
   * valueaccessor implementation; sets the value in this form control
   *
   * @param value new value
   */
  writeValue(value: number): void {
    this.value = value;
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: number) => void): void {
    this.onChange = fn;
  }

  /**
   * valueaccessor implementation; registers a callback to the form when this element has been touched
   *
   * @param fn callback
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   *  valueaccessor implementation; changes the disabled state of this control
   *
   * @param isDisabled new state
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
