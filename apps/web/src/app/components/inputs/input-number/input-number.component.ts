import {ChangeDetectorRef, Component, HostBinding, inject, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {FormsModule, NG_VALUE_ACCESSOR, NgControl} from "@angular/forms";
import { faCircleExclamation } from "@fortawesome/free-solid-svg-icons";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {Subscription} from "rxjs";

@Component({
  selector: "xsb-input-number",
  templateUrl: "./input-number.component.html",
  styleUrls: ["./input-number.component.scss"],
  standalone: true,
  imports: [
    NgClass,
    FormsModule,
    FaIconComponent
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputNumberComponent
    }
  ]
})
export class InputNumberComponent implements TypedControlValueAccessor<number>, OnInit, OnDestroy {

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
   * the native html min attribute
   */
  @Input()
  min?: number;

  /**
   * the native html max attribute
   */
  @Input()
  max?: number;

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

  /** error con */
  errorIcon = faCircleExclamation;

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

  private readonly _injector = inject(Injector);
  private _changeDetector = inject(ChangeDetectorRef);
  private _statusSubscription?: Subscription;

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
    this.ngControl = this._injector.get(NgControl);

    this._statusSubscription = this.ngControl.statusChanges?.subscribe(() => this._changeDetector.detectChanges());
  }

  ngOnDestroy() {
    this._statusSubscription?.unsubscribe();
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
