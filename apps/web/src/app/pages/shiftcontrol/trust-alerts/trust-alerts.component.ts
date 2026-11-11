import {Component, inject} from "@angular/core";
import { icons } from "../../../util/icons";
import {FormBuilder} from "@angular/forms";
import {TrustAlertDisplayDto, TrustAlertEndpointService} from "../../../../shiftservice-client";
import {debounceTime, shareReplay, startWith, switchMap} from "rxjs";
import {AsyncPipe, DatePipe} from "@angular/common";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {RouterLink} from "@angular/router";
import {MinPipe} from "../../../pipes/min.pipe";

@Component({
  selector: "app-trust-alerts",
  imports: [
    AsyncPipe,
    DatePipe,
    InputButtonComponent,
    RouterLink,
    MinPipe
  ],
  templateUrl: "./trust-alerts.component.html",
  styleUrl: "./trust-alerts.component.scss"
})
export class TrustAlertsComponent {

  protected page$;

  protected readonly form;
  protected readonly icons = icons;
  protected readonly pageSize = 20;

  private readonly _fb = inject(FormBuilder);
  private readonly _trustService = inject(TrustAlertEndpointService);

  constructor() {
    this.form = this._fb.group({
      paginationIndex: this._fb.nonNullable.control<number>(0)
    });

    this.page$ = this.form.valueChanges.pipe(
      startWith(this.form.value),
      debounceTime(100),
      switchMap((value) => this._trustService.getAllTrustAlerts(value.paginationIndex ?? 0, this.pageSize)),
      shareReplay()
    );
  }

  public dismissTrustAlert(alert: TrustAlertDisplayDto){
    this._trustService.deleteTrustAlert(alert.id).subscribe(() => {
      const currentPage = this.form.getRawValue().paginationIndex;
      this.form.patchValue({paginationIndex: currentPage});
    });
  }
}
