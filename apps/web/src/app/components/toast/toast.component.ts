import {Component, inject} from "@angular/core";
import {Toast} from "ngx-toastr";
import {icons} from "../../util/icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {Router} from "@angular/router";
import {NgClass} from "@angular/common";
import {NotificationService} from "../../services/notification/notification.service";

@Component({
  selector: "app-toast",
  imports: [
    FaIconComponent,
    NgClass
  ],
  templateUrl: "./toast.component.html",
  styleUrl: "./toast.component.scss"
})
export class ToastComponent extends Toast {

  protected readonly icons = icons;

  private readonly _router = inject(Router);
  private readonly _notificationService = inject(NotificationService);

  public get type(){
    if(this.options.payload !== undefined && this.options.payload.notification === true) {
      return "notification";
    } else {
      return "feedback";
    }
  }

  public get url(): string | undefined {
    if(this.options.payload !== undefined && typeof this.options.payload.url === "string") {
      return this.options.payload.url;
    } else {
      return undefined;
    }
  }

  close(){
    this.remove();
  }

  click() {

    if(this.url !== undefined) {
      const tree = this._router.parseUrl(this.url);
      this._router.navigateByUrl(tree);
      this._notificationService.markAllAsRead();
      return;
    }

    switch (this.type) {
      case "notification":
        this.close();
        this._router.navigateByUrl("/notifications");
        return;
      case "feedback":
        this.close();
        return;
    }
  }

}
