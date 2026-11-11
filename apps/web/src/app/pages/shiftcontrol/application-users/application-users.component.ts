import {Component, inject} from "@angular/core";
import { icons } from "../../../util/icons";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {PaginationDtoUserEventDto, UserEventEndpointService} from "../../../../shiftservice-client";
import {debounceTime, EMPTY, Observable, of, pairwise, shareReplay, startWith, switchMap} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {RouterLink} from "@angular/router";
import {InputTextComponent} from "../../../components/inputs/input-text/input-text.component";
import {TypedFormControlDirective} from "../../../directives/typed-form-control.directive";
import {MinPipe} from "../../../pipes/min.pipe";

@Component({
  selector: "app-application-users",
  standalone: true,
  imports: [
    AsyncPipe,
    InputButtonComponent,
    RouterLink,
    InputTextComponent,
    ReactiveFormsModule,
    TypedFormControlDirective,
    MinPipe
  ],
  templateUrl: "./application-users.component.html",
  styleUrl: "./application-users.component.scss"
})
export class ApplicationUsersComponent {

  protected page$: Observable<PaginationDtoUserEventDto>;

  protected readonly form;
  protected readonly icons = icons;
  protected readonly pageSize = 50;

  private readonly _fb = inject(FormBuilder);
  private readonly _userEventService = inject(UserEventEndpointService);

  constructor() {
    this.form = this._fb.group({
      paginationIndex: this._fb.nonNullable.control<number>(0),
      search: this._fb.nonNullable.control<string>("")
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
      switchMap((value) => this._userEventService.getAllUsers(value.paginationIndex ?? 0, this.pageSize, value.search, "body")),
      shareReplay()
    );
  }
}
