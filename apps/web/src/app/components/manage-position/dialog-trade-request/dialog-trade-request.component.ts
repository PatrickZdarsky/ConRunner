import {Component, EventEmitter, inject, Input, Output} from "@angular/core";
import {PositionSlotDto, TradeCandidatesDto, VolunteerDto} from "../../../../shiftservice-client";
import {BehaviorSubject, filter, map, startWith} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {DialogComponent} from "../../dialog/dialog.component";
import {icons} from "../../../util/icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {InputSelectComponent, SelectOptions} from "../../inputs/input-select/input-select.component";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {InputButtonComponent} from "../../inputs/input-button/input-button.component";

export interface tradeRequestOptions {
  slot: PositionSlotDto;
  candidates: TradeCandidatesDto[];
}

@Component({
  selector: "app-dialog-trade-request",
  imports: [
    AsyncPipe,
    DialogComponent,
    FaIconComponent,
    InputSelectComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    InputButtonComponent
  ],
  standalone: true,
  templateUrl: "./dialog-trade-request.component.html",
  styleUrl: "./dialog-trade-request.component.scss"
})
export class DialogTradeRequestComponent {

  @Output()
  public submitted = new EventEmitter<{partner: VolunteerDto; slot: PositionSlotDto} | undefined>();

  protected readonly form;
  protected readonly icons = icons;

  protected readonly requestOptions$ = new BehaviorSubject<tradeRequestOptions | undefined>(undefined);
  protected readonly offerShiftOptions$ = this.requestOptions$.pipe(
    filter(opt => opt !== undefined),
    map(opt => opt.candidates
      .map(slot => ({
        name: `${slot.ownShiftName}: ${slot.ownPosition.name}`,
        value: slot
      }))
      .sort((a, b) => a.name.localeCompare(b.name)) as SelectOptions<TradeCandidatesDto>)
  );
  protected readonly partnerOptions$;

  private readonly _fb = inject(FormBuilder);

  constructor() {
    this.form = this._fb.group({
      offeredPosition: this._fb.nonNullable.control<TradeCandidatesDto | undefined>(undefined, [Validators.required]),
      tradePartner: this._fb.nonNullable.control<VolunteerDto | undefined>(undefined, [Validators.required])
    });

    this.partnerOptions$ = this.form.controls.offeredPosition.valueChanges.pipe(
      startWith(this.form.controls.offeredPosition.value),
      map(value => {
        if(value === undefined || value === null) {
          return undefined;
        }

        return value.eligibleTradeRecipients
          .map(rec => ({
            name: `${rec.firstName} ${rec.lastName}`,
            value: rec
          }))
          .sort((a, b) => a.name.localeCompare(b.name)) as SelectOptions<VolunteerDto>;
      })
    );
  }

  @Input()
  public set requestOptions(value: tradeRequestOptions | undefined) {
    this.requestOptions$.next(value);
  }

  protected candidateComparatorFn(a: TradeCandidatesDto | null, b: TradeCandidatesDto | null): boolean {
    return a?.ownPosition?.id === b?.ownPosition?.id || (a === null && b === null);
  }

  protected volunteerComparatorFn(a: VolunteerDto | null, b: VolunteerDto | null): boolean {
    return a?.id === b?.id || (a === null && b === null);
  }

  protected selectVolunteer(options: tradeRequestOptions) {
    console.log(options);
    if(this.form.valid) {
      const offeredPosition = this.form.controls.offeredPosition.value;
      if(offeredPosition === undefined) {
        throw new Error("No offered position selected");
      }

      const volunteer = this.form.controls.tradePartner.value;
      if(volunteer === undefined) {
        throw new Error("No volunteer selected");
      }

      this.submitted.emit({
        partner: volunteer,
        slot: offeredPosition.ownPosition
      });
    }
  }

}
