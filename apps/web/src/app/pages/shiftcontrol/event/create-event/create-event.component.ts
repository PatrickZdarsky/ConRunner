import { Component } from "@angular/core";
import {ManageEventDetailsComponent} from "../../../../components/manage-event-details/manage-event-details.component";

@Component({
  selector: "app-create-event",
  imports: [
    ManageEventDetailsComponent
  ],
  templateUrl: "./create-event.component.html",
  styleUrl: "./create-event.component.scss"
})
export class CreateEventComponent {}
