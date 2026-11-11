import {Overlay, OverlayConfig, OverlayRef} from "@angular/cdk/overlay";
import { CdkPortal } from "@angular/cdk/portal";
import {AfterViewInit, Component, EventEmitter, inject, Input, OnDestroy, Output, ViewChild, ViewEncapsulation} from "@angular/core";
import {NgClass} from "@angular/common";
import {InputButtonComponent} from "../inputs/input-button/input-button.component";
import {Subscription} from "rxjs";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {icons} from "../../util/icons";
import {RouterLink} from "@angular/router";

export type dialogResult = "success" | "danger" | "close";

@Component({
  selector: "xsb-dialog",
  templateUrl: "./dialog.component.html",
  styleUrls: ["./dialog.component.scss"],
  standalone: true,
  imports: [
    NgClass,
    InputButtonComponent,
    CdkPortal,
    FaIconComponent,
    RouterLink
  ],
  encapsulation: ViewEncapsulation.None
})
export class DialogComponent implements AfterViewInit, OnDestroy {

  @Input()
  action: "danger" | "success" | "normal" = "normal";

  @Input()
  bodyStyle: "card" | "plain" = "plain";

  @Input()
  success?: string;

  @Input()
  danger?: string;

  @Input()
  expandUrl?: string;

  @Input()
  maxSize = false;

  /**
   * reference to the portal directive
   */
  @ViewChild(CdkPortal)
  public readonly portal?: CdkPortal;

  /**
   * event when the dialog should be closed
   */
  @Output()
  public readonly closeDialog = new EventEmitter<dialogResult>();

  protected readonly icons = icons;

  private readonly _overlay = inject(Overlay);

  /**
   * configuration for the overlay
   */
  private readonly _overlayConfig = new OverlayConfig({
    hasBackdrop: true,
    positionStrategy: this._overlay.position().global().centerHorizontally().centerVertically(),
    scrollStrategy: this._overlay.scrollStrategies.block(),
    panelClass: ["shiftcontrol-dialog"],
    backdropClass: ["shiftcontrol-dialog"],
  });

  /**
   * reference to the overlay
   */
  private overlayRef?: OverlayRef;
  private overlayClickSubscription?: Subscription;

  /**
   * attach the portal to the overlay
   */
  public ngAfterViewInit(): void {
    this.overlayRef = this._overlay.create(this._overlayConfig);
    this.overlayClickSubscription = this.overlayRef.backdropClick()
      .subscribe(() => {
        this.closeDialog.next("close");
      });
    this.overlayRef.attach(this.portal);
  }

  public close(){
    this.closeDialog.next("close");
  }

  /**
   * detach the portal from the overlay and dispose the overlay
   */
  public ngOnDestroy(): void {
    this.overlayRef?.detach();
    this.overlayRef?.dispose();
    this.overlayClickSubscription?.unsubscribe();
  }
}
