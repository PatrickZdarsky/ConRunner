import {Component, inject} from "@angular/core";
import { icons } from "../../../util/icons";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {LogEndpointService, LogEntryDto, PaginationDtoLogEntryDto} from "../../../../auditservice-client";
import {AsyncPipe, DatePipe} from "@angular/common";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {Observable, debounceTime, EMPTY, of, pairwise, shareReplay, startWith, switchMap} from "rxjs";
import {RouterLink} from "@angular/router";
import {InputTextComponent} from "../../../components/inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {MinPipe} from "../../../pipes/min.pipe";

@Component({
  selector: "app-audit-log",
  imports: [
    AsyncPipe,
    DatePipe,
    InputButtonComponent,
    RouterLink,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    MinPipe
  ],
  templateUrl: "./audit-log.component.html",
  styleUrl: "./audit-log.component.scss"
})
export class AuditLogComponent {
  protected readonly page$: Observable<PaginationDtoLogEntryDto>;

  protected readonly form;
  protected readonly icons = icons;
  protected readonly pageSize = 50;

  private readonly _fb = inject(FormBuilder);
  private readonly _logService = inject(LogEndpointService);

  constructor() {
    this.form = this._fb.group({
      paginationIndex: this._fb.nonNullable.control<number>(0),
      type: this._fb.nonNullable.control<string>(""),
      key: this._fb.nonNullable.control<string>("")
    });

    this.page$ = this.form.valueChanges.pipe(
      startWith(this.form.value),
      pairwise(),
      switchMap(([previousValue, value]) => {

        /* something else than page index changed, reset page */
        if(value.paginationIndex === previousValue.paginationIndex && value.paginationIndex !== 0) {
          this.form.controls.paginationIndex.setValue(0, {emitEvent: true});
          return EMPTY;
        }

        /* page index changed, keep filters */
        return of(value);
      }),
      startWith(this.form.value),
      debounceTime(100),
      switchMap((value) => {
        const eventType = value.type?.trim() || undefined;
        const routingKey = value.key?.trim() || undefined;

        return this._logService.getLogs(
          value.paginationIndex ?? 0,
          this.pageSize,
          new Date(0).toISOString(),
          new Date().toISOString(),
          undefined,
          routingKey,
          eventType
        );
      }),
      shareReplay()
    );
  }

  /**
   * Open payload json in new tab
   * @param event
   * @protected
   */
  protected viewEventDetails(event: LogEntryDto) {
    const jsonString = JSON.stringify(event.payload, null, 2);
    const blob = new Blob([jsonString], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    window.open(url, "_blank");
  }
}
