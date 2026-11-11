import {Component, inject, Input} from "@angular/core";
import {BehaviorSubject} from "rxjs";
import {EventDto, EventEndpointService} from "../../../shiftservice-client";
import { icons } from "../../util/icons";
import {ToastService} from "../../services/toast/toast.service";
import {mapValue} from "../../util/value-maps";
import {ActivatedRoute, Router} from "@angular/router";
import {AsyncPipe} from "@angular/common";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";

@Component({
  selector: "app-event-export",
  imports: [
    AsyncPipe,
    InputButtonComponent
  ],
  templateUrl: "./event-export.component.html",
  styleUrl: "./event-export.component.scss"
})
export class EventExportComponent {
  protected readonly icons = icons;
  protected readonly manageData$ = new BehaviorSubject<{ event: undefined | EventDto } | undefined>(undefined);

  private readonly _eventService = inject(EventEndpointService);
  private readonly _toastService = inject(ToastService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);

  @Input()
  public set event(value: { event: EventDto | undefined }) {
    this.manageData$.next(value);
  }

  protected clone(event: EventDto) {

    this._eventService.cloneEvent(event.id).pipe(
      this._toastService.tapSuccess("Event Clone"),
      this._toastService.tapError("Error cloning event", mapValue.apiErrorToMessage)
    ).subscribe(cloned => this._router.navigate([`../../${cloned.id}`], {relativeTo: this._route}));
  }

  protected exportEvent(event: EventDto, format: "xlsx" | "csv") {
    this._eventService.exportEventData(event.id, format).pipe(
      this._toastService.tapSuccess("Event Export"),
      this._toastService.tapError("Error exporting event", mapValue.apiErrorToMessage)
    ).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `event_${encodeURI(event.name)}_export.${format}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    });
  }
}
