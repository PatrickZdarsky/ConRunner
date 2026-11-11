import {Component, HostBinding, Injector, Input, OnInit, inject, OnDestroy, ChangeDetectorRef} from "@angular/core";
import {FormsModule, NG_VALUE_ACCESSOR, NgControl} from "@angular/forms";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faCheckCircle, faTimesCircle} from "@fortawesome/free-solid-svg-icons";
import {Subscription} from "rxjs";

@Component({
  selector: "xsb-input-toggle",
  templateUrl: "./input-toggle.component.html",
  styleUrls: ["./input-toggle.component.scss"],
  standalone: true,
  imports: [
    FormsModule,
    NgClass,
    FaIconComponent
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputToggleComponent
    }
  ]
})
export class InputToggleComponent implements TypedControlValueAccessor<boolean>, OnInit, OnDestroy {

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
   * hide from UI
   */
  @Input()
  disguise = false;

  /**
   * prohibit ID attribute on input
   */
  @Input()
  id = null;

  public readonly checkIcon = faCheckCircle;
  public readonly crossIcon = faTimesCircle;

  /** current disabled state */
  disabled = false;

  /** bound ngmodel to the underlying input */
  value = false;

  /** inchange callback to be set by the parent form */
  onChange?: (value: boolean) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  private injector = inject(Injector);
  private _changeDetector = inject(ChangeDetectorRef);
  private _statusSubscription?: Subscription;

  /* hide actual properties from html to prevent accessibility isues */
  @HostBinding("attr.name") get hideNameAttr() { return null; }
  @HostBinding("attr.id") get hideIdAttr() { return null; }
  @HostBinding("attr.value") get hideValueAttr() { return null; }

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
  writeValue(value: boolean): void {
    this.value = value;
    this._changeDetector.markForCheck();
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: boolean) => void): void {
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
