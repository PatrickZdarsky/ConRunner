import {Component, HostBinding, Injector, Input, OnInit, inject, ChangeDetectorRef, OnDestroy} from "@angular/core";
import {FormsModule, NG_VALUE_ACCESSOR, NgControl} from "@angular/forms";
import { faCircleExclamation } from "@fortawesome/free-solid-svg-icons";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {Subscription} from "rxjs";

@Component({
  selector: "xsb-input-text",
  templateUrl: "./input-text.component.html",
  styleUrls: ["./input-text.component.scss"],
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
      useExisting: InputTextComponent
    }
  ]
})
export class InputTextComponent implements TypedControlValueAccessor<string>, OnInit, OnDestroy {


  /**
   * the size style of the input. minimal will have less border and padding
   */
  @Input()
  size: "minimal" | "regular" = "regular";

  /**
   * HTML type of the input element
   */
  @Input()
  type: "text" | "email" | "password" | "area" = "text";

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
  value = "";

  /** onchange callback to be set by the parent form */
  onChange?: (value: string) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  private _injector = inject(Injector);
  private _changeDetector = inject(ChangeDetectorRef);
  private _statusSubscription?: Subscription;

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
  writeValue(value: string): void {
    this.value = value;
    this._changeDetector.markForCheck();
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: string) => void): void {
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
