import { Component, inject } from "@angular/core";
import {FormBuilder, ReactiveFormsModule} from "@angular/forms";
import {InputButtonComponent} from "../../../components/inputs/input-button/input-button.component";
import {UserService} from "../../../services/user/user.service";
import {ActivatedRoute, Router} from "@angular/router";
import {filter, take} from "rxjs";

@Component({
  selector: "app-login",
  imports: [
    ReactiveFormsModule,
    InputButtonComponent
  ],
  standalone: true,
  templateUrl: "./login.component.html",
  styleUrl: "./login.component.scss"
})
export class LoginComponent {
  public readonly form;

  private readonly _userService = inject(UserService);
  private readonly _fb = inject(FormBuilder);
  private readonly _activatedRoute = inject(ActivatedRoute);
  private readonly _router = inject(Router);

  constructor(){
    this.form = this._fb.group({
      email: this._fb.nonNullable.control<string>(""),
      password: this._fb.nonNullable.control<string>(""),
    });

    this._userService.kcProfile$.pipe(
      filter(profile => profile !== null),
      take(1)
    ).subscribe(() => {
      const continueParam = this._activatedRoute.snapshot.queryParamMap.get("continue");
      const decodedContinue = continueParam ? decodeURIComponent(continueParam) : "/";

      if (decodedContinue.startsWith("http://") || decodedContinue.startsWith("https://")) {
        window.location.href = decodedContinue;
        return;
      }

      void this._router.navigateByUrl(decodedContinue);
    });
  }

  public login() {
    const continueParam = this._activatedRoute.snapshot.queryParamMap.get("continue");
    const decodedContinue = continueParam ? decodeURIComponent(continueParam) : undefined;

    this._userService.login(decodedContinue);
  }
}
