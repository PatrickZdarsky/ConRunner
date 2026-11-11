import {Component, inject, OnDestroy} from "@angular/core";
import {PageService} from "../../../services/page/page.service";
import {ActivatedRoute, Router} from "@angular/router";
import {UserEventDto, UserEventEndpointService} from "../../../../shiftservice-client";
import {BehaviorSubject, filter,  Subscription} from "rxjs";
import {BC_VOLUNTEER} from "../../../breadcrumbs";
import {AsyncPipe} from "@angular/common";
import {ManageVolunteerComponent} from "../../../components/manage-volunteer/manage-volunteer.component";

@Component({
  selector: "app-volunteer",
  imports: [
    AsyncPipe,
    ManageVolunteerComponent
  ],
  templateUrl: "./volunteer.component.html",
  styleUrl: "./volunteer.component.scss"
})
export class VolunteerComponent implements OnDestroy {

  protected readonly volunteer$ = new BehaviorSubject<UserEventDto | undefined>(undefined);

  private readonly _pageService = inject(PageService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _userEventService = inject(UserEventEndpointService);
  private readonly _pageSubscription: Subscription;

  constructor() {
    const volunteerId = this._route.snapshot.paramMap.get("volunteerId");
    if(volunteerId === null) {
      this._router.navigateByUrl("/");
      throw new Error("Volunteer ID is required");
    }

    this.reloadVolunteer(volunteerId);

    this._pageSubscription = this.volunteer$.pipe(
      filter(v => v !== undefined)
    ).subscribe(v => {
      this._pageService.configurePageName(`${v.volunteer.firstName} ${v.volunteer.lastName}`);
      this._pageService.configureBreadcrumb(BC_VOLUNTEER,
        `${v.volunteer.firstName} ${v.volunteer.lastName.substring(0,1)}.`, `${v.volunteer.id}`
      );
    });
  }

  ngOnDestroy() {
    this._pageSubscription.unsubscribe();
  }

  protected reloadVolunteer(id: string){
    this._userEventService.getUser(id).subscribe(updatedVolunteer => {
      this.volunteer$.next(updatedVolunteer);
    });
  }
}
