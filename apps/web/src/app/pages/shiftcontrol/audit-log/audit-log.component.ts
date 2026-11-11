/* eslint-disable @typescript-eslint/member-ordering -- injected dependencies initialize fields below */
import {Component, inject} from "@angular/core";
import {AsyncPipe, DatePipe} from "@angular/common";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {debounceTime, startWith, switchMap} from "rxjs";
import {AuditEndpointService} from "../../../../api-client";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {InputTextComponent} from "../../../components/inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";

@Component({
  selector: "app-audit-log",
  imports: [AsyncPipe, DatePipe, ReactiveFormsModule, InputButtonComponent, InputTextComponent, TypedFormControlDirective],
  templateUrl: "./audit-log.component.html", styleUrl: "./audit-log.component.scss"
})
export class AuditLogComponent {
  private readonly api = inject(AuditEndpointService);
  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({page: 0, module: "", action: ""});
  protected readonly page$ = this.form.valueChanges.pipe(startWith(this.form.getRawValue()), debounceTime(100),
    switchMap(value => this.api.search(value.page ?? 0, 50, undefined, undefined, undefined,
      value.module?.trim() || undefined, value.action?.trim() || undefined)));
}
