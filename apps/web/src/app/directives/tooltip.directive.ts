import {ComponentRef, Directive, ElementRef, HostListener, inject, Input, OnDestroy, OnInit} from "@angular/core";
import {Overlay, OverlayPositionBuilder, OverlayRef} from "@angular/cdk/overlay";
import {TooltipComponent} from "../components/tooltip/tooltip.component";
import {ComponentPortal} from "@angular/cdk/portal";

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: "[app-tooltip]",
  standalone: true
})
export class TooltipDirective implements OnInit, OnDestroy {

  @Input()
  public tooltipMaxWidth: string = "20rem";

  @Input("app-tooltip") text = "";
  private overlayRef?: OverlayRef;


  private readonly overlay = inject(Overlay);
  private readonly overlayPositionBuilder = inject(OverlayPositionBuilder);
  private readonly elementRef = inject(ElementRef);


  @HostListener("pointerenter")
  show() {
    if(!this.overlayRef) {
      return;
    }

    const tooltipRef: ComponentRef<TooltipComponent>
      = this.overlayRef.attach(new ComponentPortal(TooltipComponent));
    tooltipRef.instance.text = this.text;
    tooltipRef.instance.maxWidth = this.tooltipMaxWidth;
  }

  @HostListener("pointerleave")
  hide() {
    this.overlayRef?.detach();
  }

  ngOnInit(): void {
    const positionStrategy = this.overlayPositionBuilder
      .flexibleConnectedTo(this.elementRef)
      .withPositions([{
        originX: "center",
        originY: "top",
        overlayX: "center",
        overlayY: "bottom",
        offsetY: -8,
      }]);

    this.overlayRef = this.overlay.create({ positionStrategy });
  }

  ngOnDestroy() {
    this.overlayRef?.dispose();
  }
}
