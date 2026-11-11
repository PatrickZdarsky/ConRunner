import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostBinding, inject,
  Injector,
  Input,
  OnInit
} from "@angular/core";
import { NG_VALUE_ACCESSOR, NgControl } from "@angular/forms";
import {faCaretDown, faCircleCheck} from "@fortawesome/free-solid-svg-icons";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {NgClass} from "@angular/common";

export type MultiToggleOptions<TData> = Array<{ name: string; value: TData }>;

@Component({
  selector: "xsb-input-multitoggle",
  templateUrl: "./input-multi-toggle.component.html",
  styleUrls: ["./input-multi-toggle.component.scss"],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputMultiToggleComponent
    }
  ],
  imports: [
    NgClass
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InputMultiToggleComponent<TData> implements TypedControlValueAccessor<TData | null>, OnInit {

  /**
   * the selection items that can be chosen of
   */
  @Input()
  options: MultiToggleOptions<TData> = [];

  /**
   * border display
   */
  @Input()
  border = true;

  /**
   * whether "no-selection" should be allowed
   */
  @Input()
  nullable = false;

  @Input()
  nullName = " - ";

  @Input()
  size: "regular" | "minimal" | "text" = "regular";

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
  private changeDetectorRef = inject(ChangeDetectorRef);

  @HostBinding("attr.id") get hideIdAttr() { return null; }
  @HostBinding("attr.name") get hideNameAttr() { return null; }

  /**
   * get the current display name
   *
   * @returns name or empty indicator
   */
  get currentValueName() {
    const opt = this.options.find(o => o.value === this.value);
    return opt?.name ?? this.nullName;
  }

  /**
   * current validity state determined by validators
   */
  get valid() {
    return this.ngControl ? !this.ngControl.invalid : true;
  }

  get allOptions() {
    return [ ...(this.nullable ? [{ value: null, name: this.nullName }] : []), ...this.options];
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
  writeValue(value: TData | null): void {
    const match = this.options.find(o => o.value === value);
    if (match !== undefined) {
      this.value = value;
      this.changeDetectorRef.markForCheck();
    } else {
      const oldValue = this.value;
      this.value = this.nullable || this.options.length === 0 ? null : this.options[0].value;

      if(oldValue === this.value) {return;}
      if (this.onChange !== undefined) {
        this.onChange(this.value);
        this.changeDetectorRef.markForCheck();
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
  selectValue(value: TData | null) {
    this.value = value;
    this.onTouched?.();
    this.onChange?.(value);
  }
}
