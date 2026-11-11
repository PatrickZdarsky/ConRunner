import {Directive, ElementRef, HostListener, inject, Input, OnDestroy, ViewContainerRef} from "@angular/core";
import { Overlay, OverlayRef } from "@angular/cdk/overlay";
import { Subscription, Observable, merge } from "rxjs";
import { TemplatePortal } from "@angular/cdk/portal";
import { FlyoutComponent } from "../components/flyout/flyout.component";


@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: "[xsb-flyout]"
})
export class FlyoutTriggerDirective<TTemplate> implements OnDestroy {

  @Input({alias: "xsb-flyout"}) public flyout!: FlyoutComponent<TTemplate>;

  private overlay = inject(Overlay);
  private elementRef = inject(ElementRef<HTMLElement>);
  private viewContainerRef = inject(ViewContainerRef);

  private isDropdownOpen = false;
  private overlayRef?: OverlayRef;
  private dropdownClosingActionsSub = Subscription.EMPTY;

  @HostListener("click", ["$event.target"])
  toggleDropdown(): void {
    if (this.isDropdownOpen) {
      this.destroyDropdown();
    } else {
      this.openDropdown();
    }
  }

  openDropdown(): void {
    this.isDropdownOpen = true;
    this.overlayRef = this.overlay.create({
      hasBackdrop: true,
      backdropClass: "cdk-overlay-transparent-backdrop",
      scrollStrategy: this.overlay.scrollStrategies.close(),
      positionStrategy: this.overlay
        .position()
        .flexibleConnectedTo(this.elementRef)
        .withPositions([
          {
            originX: "end",
            originY: "bottom",
            overlayX: "end",
            overlayY: "top",
            offsetY: 5
          }
        ])
    });

    const templatePortal = new TemplatePortal(
      this.flyout.templateRef,
      this.viewContainerRef
    );
    this.overlayRef.attach(templatePortal);

    this.dropdownClosingActionsSub = this.dropdownClosingActions().subscribe(
      () => this.destroyDropdown()
    );
  }

  ngOnDestroy(): void {
    if (this.overlayRef) {
      this.overlayRef.dispose();
    }
  }

  private dropdownClosingActions(): Observable<MouseEvent | void> {

    if (this.overlayRef === undefined) {
      throw new Error("overlay not yet initialized");
    }

    const backdropClick$ = this.overlayRef.backdropClick();
    const detachment$ = this.overlayRef?.detachments();
    const dropdownClosed = this.flyout?.closed;

    return merge(backdropClick$, detachment$, dropdownClosed);
  }

  private destroyDropdown(): void {
    if (!this.overlayRef || !this.isDropdownOpen) {
      return;
    }

    this.dropdownClosingActionsSub.unsubscribe();
    this.isDropdownOpen = false;
    this.overlayRef.detach();
  }
}
