import {Component, inject} from "@angular/core";
import { RouterOutlet } from "@angular/router";
import {PageComponent} from "./components/page/page.component";
import {UserService} from "./services/user/user.service";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [RouterOutlet, PageComponent],
  templateUrl: "./app.component.html",
  styleUrl: "./app.component.scss"
})
export class AppComponent {
  title = "ShiftControl";

  // inject user service to start logging
  private readonly userService = inject(UserService);
}
