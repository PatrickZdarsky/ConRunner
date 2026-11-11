import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostBinding,
  inject,
  Injector,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import { NG_VALUE_ACCESSOR, NgControl } from "@angular/forms";
import {faCaretDown, faCircleCheck, IconDefinition} from "@fortawesome/free-solid-svg-icons";
import { SelectOptions } from "../input-select/input-select.component";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {FlyoutTriggerDirective} from "../../../directives/flyout-trigger.directive";
import {NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {FlyoutComponent} from "../../flyout/flyout.component";
import {merge, of, Subscription} from "rxjs";

@Component({
  selector: "xsb-input-multiselect",
  templateUrl: "./input-multiselect.component.html",
  styleUrls: ["./input-multiselect.component.scss"],
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputMultiselectComponent
    }
  ],
  imports: [
    FlyoutTriggerDirective,
    NgClass,
    FaIconComponent,
    FlyoutComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InputMultiselectComponent<TData> implements TypedControlValueAccessor<TData[] | null>, OnInit, OnDestroy {

  /**
   * the selection items that can be chosen of
   */
  @Input()
  options: SelectOptions<TData> = [];

  /**
   * list all selected names in preview instead of count
   */
  @Input()
  previewWholeSelection = false;

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

  /**
   * disguise the input as plain text and make immutable
   */
  @Input()
  disguise = false;

  /**
   * plural naming for display
   */
  @Input({ required: true })
  naming!: string;

  /**
   * empty naming for display
   */
  @Input()
  emptyNaming?: string;

  /**
   * prohibit ID attribute on input
   */
  @Input()
  id = null;

  /**
   * the input's name, will reflect as id on the actual element
   */
  @Input()
  name = "";

  /** current disabled state */
  disabled = false;

  /** bound ngmodel to the underlying input */
  value: TData[] | null = null;

  /** onchange callback to be set by the parent form */
  onChange?: (value: TData[] | null) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  /** whether the init value was modified to something else  */
  initModified?: TData[] | null;

  caretDownIcon = faCaretDown;
  circleCheckIcon = faCircleCheck;

  private injector = inject(Injector);
  private _changeDetector = inject(ChangeDetectorRef);
  private _statusSubscription?: Subscription;

  @HostBinding("attr.id") get hideIdAttr() { return null; }
  @HostBinding("attr.name") get hideNameAttr() { return null; }


  /**
   * get the current display name
   *
   * @returns name or empty indicator
   */
  get currentValueName() {
    const opt = this.options.filter(o => this.value?.some(v => this.comparatorFn(v, o.value)));
    return opt.length === 0 ?
      (this.emptyNaming ?? `No ${this.naming}`) :
      (opt.length === 1 ? opt[0].name :
        this.previewWholeSelection ? opt.map(o => o.name).join(", ") : `${opt.length} ${this.naming}`);
  }

  /**
   * current validity state determined by validators
   */
  get valid() {
    return this.ngControl ? this.ngControl.valid : true;
  }

  get allOptions() {
    return this.options;
  }

  @Input()
  comparatorFn: ((a: TData | null, b: TData | null) => boolean) = (a, b) => a === b;

  isSelected(value: TData | null) {
    return (this.value ?? []).some(o => this.comparatorFn(o, value));
  }

  /**
   * get the form control (self) avoiding circular deps
   */
  ngOnInit(): void {
    this.ngControl = this.injector.get(NgControl);

    this._statusSubscription = merge(
      this.ngControl.valueChanges ?? of(undefined),
      this.ngControl.statusChanges ?? of(undefined)
    ).subscribe(() => this._changeDetector.detectChanges());
  }

  ngOnDestroy() {
    this._statusSubscription?.unsubscribe();
  }

  /**
   * valueaccessor implementation; sets the value in this form control
   *
   * @param value new value
   */
  writeValue(value: TData[] | null): void {
    this.value = value?.filter(v => this.options.some(o => this.comparatorFn(o.value, v))) ?? null;
    this._changeDetector.markForCheck();
    if (this.onChange !== undefined) {
      this.onChange(this.value);
    } else {
      this.initModified = this.value;
    }
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: TData[] | null) => void): void {
    this.onChange = fn;

    /* check if init value has changed before onchange was registered */
    if (this.initModified !== undefined) {
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
  }

  /**
   * apply a value and emit changes
   *
   * @param value the new value
   */
  toggleValue(value: TData | null) {
    const active = this.isSelected(value);
    if (value !== null && !active) {
      this.value = this.value === null ? [value] : [...this.value, value];
    } else {
      this.value = this.value === null ? null : this.value.filter(v => !this.comparatorFn(v, value) );
    }
    this.onTouched?.();
    this.onChange?.(this.value);
  }
}
