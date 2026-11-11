import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostBinding, inject,
  Injector,
  Input, OnDestroy,
  OnInit
} from "@angular/core";
import { NG_VALUE_ACCESSOR, NgControl } from "@angular/forms";
import {faCaretDown, faCircleCheck, faCircleExclamation, IconDefinition} from "@fortawesome/free-solid-svg-icons";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {NgClass} from "@angular/common";
import {FlyoutComponent} from "../../flyout/flyout.component";
import {FlyoutTriggerDirective} from "../../../directives/flyout-trigger.directive";
import {Subscription} from "rxjs";

export type SelectOptions<TData> = Array<{ name: string; value: TData }>;

@Component({
  selector: "xsb-input-select",
  templateUrl: "./input-select.component.html",
  styleUrls: ["./input-select.component.scss"],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputSelectComponent
    }
  ],
  imports: [
    FaIconComponent,
    NgClass,
    FlyoutComponent,
    FlyoutTriggerDirective
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InputSelectComponent<TData> implements TypedControlValueAccessor<TData | null>, OnInit, OnDestroy {


  /**
   * border display
   */
  @Input()
  border = true;

  /**
   * the icon to show left to the current value
   */
  @Input()
  icon?: IconDefinition;

  /**
   * whether "no-selection" should be allowed
   */
  @Input()
  nullable = false;

  @Input()
  nullName = " - ";

  /**
   * disguise the input as plain text and make immutable
   */
  @Input()
  disguise = false;

  /**
   * the input's name, will reflect as id on the actual element
   */
  @Input()
  name = "";

  /**
   * prohibit ID attribute on input
   */
  @Input()
  id = null;

  /** current disabled state */
  disabled = false;

  /** bound ngmodel to the underlying input */
  value: TData | null = null;

  /** error con */
  errorIcon = faCircleExclamation;

  /** onchange callback to be set by the parent form */
  onChange?: (value: TData | null) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  /** whether the init value was modified to something else  */
  initModified?: TData | null;

  caretDownIcon = faCaretDown;
  circleCheckIcon = faCircleCheck;

  private injector = inject(Injector);
  private _changeDetector = inject(ChangeDetectorRef);
  private _statusSubscription?: Subscription;
  private _options: SelectOptions<TData> = [];

  @HostBinding("attr.id") get hideIdAttr() { return null; }
  @HostBinding("attr.name") get hideNameAttr() { return null; }

  /**
   * get the current display name
   *
   * @returns name or empty indicator
   */
  get currentValueName() {
    const opt = this.options.find(o => this.comparatorFn(this.value, o.value));
    return opt?.name ?? this.nullName;
  }

  /**
   * current validity state determined by validators
   */
  get valid() {
    return this.ngControl ? !this.ngControl.invalid : true;
  }

  get allOptions() {
    return [...this.options, ...(this.nullable ? [{ value: null, name: this.nullName }] : [])];
  }

  get options(){
    return this._options;
  }

  /**
   * the selection items that can be chosen of
   */
  @Input()
  set options(value: SelectOptions<TData>) {
    this._options = value;
    this.writeValue(this.value);
  }

  @Input()
  comparatorFn: ((a: TData | null, b: TData | null) => boolean) = (a, b) => a === b;

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
  writeValue(value: TData | null): void {
    const match = this.options.find(o => this.comparatorFn(o.value, value));
    if (match !== undefined) {
      this.value = value;
      this._changeDetector.markForCheck();
    } else {
      const oldValue = this.value;
      this.value = this.nullable || this.options.length === 0 ? null : this.options[0].value;

      if(oldValue === this.value) {return;}
      if (this.onChange !== undefined) {
        this.onChange(this.value);
        this._changeDetector.markForCheck();
      } else {
        this.initModified = this.value;
      }
    }
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: TData | null) => void): void {
    this.onChange = fn;

    /* check if init value has changed before onchange was registered */
    if (this.initModified !== undefined && this.comparatorFn(this.value, this.initModified)) {
      this.onChange(this.initModified);
    }
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
    this._changeDetector.detectChanges();
  }

  /**
   * apply a value and emit changes
   *
   * @param value the new value
   */
  selectValue(value: TData | null) {
    this.value = value;
    this.onTouched?.();
    this.onChange?.(value);
  }
}
