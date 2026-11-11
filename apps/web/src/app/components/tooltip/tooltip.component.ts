import {ChangeDetectionStrategy, Component, Input} from "@angular/core";
import { animate, style, transition, trigger } from "@angular/animations";

@Component({
  selector: "app-tooltip",
  imports: [],
  standalone: true,
  templateUrl: "./tooltip.component.html",
  styleUrl: "./tooltip.component.scss",
  animations: [
    trigger("tooltip", [
      transition(":enter", [
        style({
          opacity: 0,
          transform: "scaleY(0)",
          transformOrigin: "bottom"
        }),
        animate(
          "80ms ease-out",
          style({
            opacity: 1,
            transform: "scaleY(1)"
          })
        )
      ]),
      transition(":leave", [
        animate(
          "80ms ease-in",
          style({
            opacity: 0,
            transform: "scaleY(0)"
          })
        )
      ])
    ])
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TooltipComponent {

  @Input() text?: string;

  @Input() maxWidth?: string = "10rem";
}
