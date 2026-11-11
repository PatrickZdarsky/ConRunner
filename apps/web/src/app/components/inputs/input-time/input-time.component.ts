import {Component, HostBinding, Injector, Input, OnInit, inject, ChangeDetectorRef} from "@angular/core";
import {FormsModule, NG_VALUE_ACCESSOR, NgControl} from "@angular/forms";
import { faCircleExclamation } from "@fortawesome/free-solid-svg-icons";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";

export interface time {
  hour: number;
  minute: number;
}

@Component({
  selector: "xsb-input-time",
  templateUrl: "./input-time.component.html",
  styleUrls: ["./input-time.component.scss"],
  standalone: true,
  imports: [
    NgClass,
    FaIconComponent,
    FormsModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputTimeComponent
    }
  ]
})
export class InputTimeComponent implements TypedControlValueAccessor<time>, OnInit {


  /**
   * the size style of the input. minimal will have less border and padding
   */
  @Input()
  size: "minimal" | "regular" = "regular";

  /**
   * the native html for attribute for accessibility
   */
  @Input()
  name = "";

  /**
   * disguise the input as plain text and make immutable
   */
  @Input()
  disguise = false;

  /**
   * prohibit ID attribute on input
   */
  @Input()
  id = null;

  /**
   * HTML placeholder of the input element
   */
  @Input()
  placeholder = "";

  /** error con */
  errorIcon = faCircleExclamation;

  /** current disabled state */
  disabled = false;

  /** bound ngmodel to the underlying input */
  hour = 0;
  minute = 0;

  /** onchange callback to be set by the parent form */
  onChange?: (value: time) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  private injector = inject(Injector);
  private _changeDetectorRef = inject(ChangeDetectorRef);

  /* hide actual properties from html to prevent accessibility issues */
  @HostBinding("attr.name") get hideNameAttr() { return null; }
  @HostBinding("attr.id") get hideIdAttr() { return null; }
  @HostBinding("attr.type") get hideTypeAttr() { return null; }
  @HostBinding("attr.value") get hideValueAttr() { return null; }
  @HostBinding("attr.placeholder") get hidePlaceholderAttr() { return null; }

  /**
   * current validity state determined by validators
   */
  get valid() {
    return this.ngControl ? this.ngControl.valid || !this.ngControl.touched : true;
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
  writeValue(value: time): void {
    this.hour = Math.min(23, Math.max(value.hour, 0));
    this.minute = Math.min(59, Math.max(value.minute, 0));
    this._changeDetectorRef.markForCheck();
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: time) => void): void {
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

  clampTime(time: time) {
    time.hour = Math.min(23, Math.max(time.hour, 0));
    time.minute = Math.min(59, Math.max(time.minute, 0));
    return time;
  }

  validateTime(){
    if(this.minute >= 60 || this.minute < 0 || this.hour < 0 || this.hour >= 24) {
      this.writeValue({minute: this.minute, hour: this.hour});
    }
  }
}
