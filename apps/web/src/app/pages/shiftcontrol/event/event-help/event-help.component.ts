import {Component, inject} from "@angular/core";
import {PageService} from "../../../../services/page/page.service";
import { icons } from "../../../../util/icons";
import {EventEndpointService} from "../../../../../shiftservice-client";
import {BC_EVENT} from "../../../../breadcrumbs";
import {ActivatedRoute, Router} from "@angular/router";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {AsyncPipe} from "@angular/common";

@Component({
  selector: "app-event-help",
  imports: [
    FaIconComponent,
    AsyncPipe
  ],
  templateUrl: "./event-help.component.html",
  styleUrl: "./event-help.component.scss"
})
export class EventHelpComponent {

  protected readonly help$;

  protected readonly icons = icons;

  private readonly _pageService = inject(PageService);
  private readonly _eventService = inject(EventEndpointService);
  private readonly _router = inject(Router);
  private readonly _route = inject(ActivatedRoute);

  constructor() {
    const eventId = this._route.snapshot.paramMap.get("eventId");
    if(eventId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Event ID is required");
    }

    this.help$ = this._eventService.getPlannerContacts(eventId);

    this._eventService.getShiftPlansOverviewOfEvent(eventId).subscribe(event => {
      this._pageService
        .configurePageName(`${event.eventOverview.name}`)
        .withCalendarLink("../calendar")
        .configureBreadcrumb(BC_EVENT, event.eventOverview.name, event.eventOverview.id);
    });
  }

}
