import { Component, EventEmitter, Input, Output, TemplateRef, ViewChild } from "@angular/core";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faXmark} from "@fortawesome/free-solid-svg-icons";

@Component({
  selector: "xsb-flyout",
  standalone: true,
  templateUrl: "./flyout.component.html",
  imports: [
    FaIconComponent
  ],
  styleUrls: ["./flyout.component.scss"]
})
export class FlyoutComponent<TTemplate> {

  /**
   * the template element
   */
  @ViewChild(TemplateRef)
  templateRef!: TemplateRef<TTemplate>;

  /**
   * event emitter for when the flyout should close
   */
  @Output()
  closed = new EventEmitter<void>();

  /**
   * the title of the flyout
   */
  @Input()
  title?: string;

  /**
   * sets whether the flyout should be closed on inbound click
   */
  @Input()
  closeOnClick = true;

  xmarkIcon = faXmark;

  /**
   * close the flyout
   */
  close() {
    this.closed.next();
  }
}
