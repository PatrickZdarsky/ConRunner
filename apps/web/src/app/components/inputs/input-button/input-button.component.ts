import {Component, EventEmitter, HostBinding, Input, Output} from "@angular/core";
import { IconDefinition, faGear } from "@fortawesome/free-solid-svg-icons";
import {NgClass} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";

@Component({
  selector: "xsb-input-button",
  standalone: true,
  templateUrl: "./input-button.component.html",
  imports: [
    NgClass,
    FaIconComponent
  ],
  styleUrls: ["./input-button.component.scss"]
})
export class InputButtonComponent {

  /**
   * event when the button had been clicked
   */
  @Output()
  public readonly xsbClick = new EventEmitter<MouseEvent>();

  @Input()
  size: "minimal" | "regular" | "text" = "regular";

  @Input()
  action: "danger" | "normal" | "success" = "normal";

  @Input()
  type: "button" | "submit" = "button";

  @Input()
  name = "";

  @Input()
  content = "";

  @Input()
  icon?: IconDefinition;

  @Input()
  lock = false;

  @Input()
  preventDefault = true;

  @Input()
  disableLockIcon = false;

  lockIcon = faGear;

  /* hide actual properties from html to prevent accessibility isues */
  @HostBinding("attr.name") get hideNameAttr() { return null; }
  @HostBinding("attr.type") get hideTypeAttr() { return null; }

  clicked(event: MouseEvent) {
    if(this.preventDefault){
      event.preventDefault();
      event.stopPropagation();
    }
    if(!this.lock) {this.xsbClick.emit(event);}
  }
}
