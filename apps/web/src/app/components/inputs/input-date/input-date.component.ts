import {
  AfterViewInit,
  ChangeDetectionStrategy, ChangeDetectorRef,
  Component,
  ElementRef,
  HostBinding, inject,
  Injector,
  Input, OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {TypedControlValueAccessor} from "../../../util/typedControlValueAccessor";
import {NG_VALUE_ACCESSOR, NgControl} from "@angular/forms";
import {faCalendar, faCaretDown, faChevronLeft, faChevronRight} from "@fortawesome/free-solid-svg-icons";
import {DateTime} from "luxon";
import {FlyoutComponent} from "../../flyout/flyout.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {NgClass} from "@angular/common";
import {FlyoutTriggerDirective} from "../../../directives/flyout-trigger.directive";
import {Subscription} from "rxjs";

@Component({
  selector: "xsb-input-date",
  templateUrl: "./input-date.component.html",
  styleUrls: ["./input-date.component.scss"],
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputDateComponent
    }
  ],
  imports: [
    FlyoutComponent,
    FlyoutTriggerDirective,
    FaIconComponent,
    NgClass
  ],
   changeDetection: ChangeDetectionStrategy.OnPush
})
export class InputDateComponent implements TypedControlValueAccessor<Date | null>, OnInit, OnDestroy, AfterViewInit {

  @ViewChild("wrapper", { static: false })
  wrapper?: ElementRef<HTMLDivElement>;

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
   * prohibit ID attribute on input
   */
  @Input()
  id = null;

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

  @Input()
  nullable = false;

  dateIcon = faCalendar;
  chevronLeftIcon = faChevronLeft;
  chevronRightIcon = faChevronRight;
  caretDownIcon = faCaretDown;

  /** current disabled state */
  disabled = false;

  /** onchange callback to be set by the parent form */
  onChange?: (value: Date | null) => void;

  /** ontouched callback to be set by the parent form */
  onTouched?: () => void;

  /** self form control to access validity state */
  ngControl?: NgControl;

  private _position: DateTime;
  private _value: DateTime | null;
  private _injector = inject(Injector);
  private _changeDetectorRef = inject(ChangeDetectorRef);
  private _changeSubscription?: Subscription;

  constructor() {
    this._value = this.nullable ? null : DateTime.local();
    this._position = this.value === null ? DateTime.local() : this.value;
  }

  /* hide actual properties from html to prevent accessibility issues */
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

  get dateText() {
    return this.value === null ? " - " : this.value.setLocale("en-UK").toLocaleString(DateTime.DATE_SHORT);
  }

  get currentMonth() {
    return this.position.setLocale("en-UK").toFormat("MMMM");
  }

  get currentYear() {
    return this.position.toFormat("yyyy");
  }

  /** current value */
  get value() { return this._value; }

  /** current position */
  get position() {
    return this._position;
  }

  set value(value: DateTime | null) {
    this._value = value;
  }

  set position(value: DateTime) {
    this._position = value;
  }

  createDateGrid(date: DateTime) {
    const startOfMonth = date.startOf("month");
    const endOfMonth = date.endOf("month");
    const startOfCalendar = startOfMonth.startOf("week");
    const endOfCalendar = endOfMonth.endOf("week");

    const calendarMonth: { date: DateTime; dayOfMonth: string }[] = [];

    let currentDate = startOfCalendar;
    while (currentDate <= endOfCalendar) {
      calendarMonth.push({
        date: currentDate,
        dayOfMonth: currentDate.toFormat("d")
      });

      currentDate = currentDate.plus({ days: 1 });
    }

    return calendarMonth;
  }

  isChosen(date: DateTime) {
    return this.value !== null && this.value.hasSame(date, "day");
  }

  navigateYear(positive = true) {
    this.position = this.position.plus({ years: 1 * (positive ? 1 : -1) });
  }

  navigateMonth(positive = true) {
    this.position = this.position.plus({ months: 1 * (positive ? 1 : -1) });
  }

  /**
   * get the form control (self) avoiding circular deps
   */
  ngOnInit(): void {
    this.ngControl = this._injector.get(NgControl);
    this._changeSubscription = this.ngControl.statusChanges?.subscribe(() => this._changeDetectorRef.markForCheck());
  }

  ngOnDestroy() {
    this._changeSubscription?.unsubscribe();
  }

  ngAfterViewInit(): void {
    return;
    /* const observer = new MutationObserver((mutationsList) => {
      for (const mutation of mutationsList) {
        if (mutation.type === 'attributes' && mutation.attributeName === 'value') {
          const attr = this.wrapper?.nativeElement.getAttribute('value') ?? null;

          if (attr == this.dateText) return;
          const date = attr === null ? null : DateTime.fromFormat(attr, 'd.M.yyyy', { locale: 'de-at' });
          if (date?.isValid) this.pickDate(date);
          else this.pickDate(null);

          this.position = this.value === null ? DateTime.local() : this.value;
        }
      }
    });

    if (this.wrapper?.nativeElement) observer.observe(this.wrapper?.nativeElement, { attributes: true });*/
  }

  /**
   * valueaccessor implementation; sets the value in this form control
   *
   * @param value new value
   */
  writeValue(value: Date | null): void {
    this.value = value === null ? null : DateTime.fromJSDate(value);
    this.position = this.value === null ? DateTime.local() : this.value;
    this._changeDetectorRef.markForCheck();
  }

  /**
   * valueaccessor implementation; sets a callback to the form when this value has changes
   *
   * @param fn callback
   */
  registerOnChange(fn: (value: Date | null) => void): void {
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

  pickDate(date: DateTime | null) {
    this.value = date;
    const jsDate = date === null ? null : date.toJSDate();
    this.onChange?.(jsDate);
  }
}

