import { Pipe, PipeTransform } from "@angular/core";
import {ShiftDto} from "../../shiftservice-client";
import LockStatusEnum = ShiftDto.LockStatusEnum;

@Pipe({
  name: "lockStatus"
})
export class LockStatusPipe implements PipeTransform {

  transform(value: LockStatusEnum, mode: "name" | "description" | "info" = "name"): string {
    switch (mode) {
      case "description":
        switch (value) {
          case LockStatusEnum.Locked:
            return "Volunteers can take no actions to change their assignments. Planners can still manually adjust assignments.";
          case LockStatusEnum.SelfSignup:
            return "Volunteers can freely sign up and remove themselves from shifts.";
          case LockStatusEnum.Supervised:
            return "No changes to assignment count can be made. " +
              "Volunteers can only trade positions. Planners can still manually adjust assignments.";
          default:
            return "Unknown";
        }
      case "info":
        switch (value) {
          case LockStatusEnum.Locked:
            return "Shift sign-ups can no longer be changed.";
          case LockStatusEnum.SelfSignup:
            return "You can sign-up and withdraw from shifts freely.";
          case LockStatusEnum.Supervised:
            return "You can only sign-up or withdraw from shifts with approval from a manager or by trading with other volunteers.";
          default:
            return "Unknown";
        }
      case "name":
        switch (value) {
          case LockStatusEnum.Locked:
            return "Locked";
          case LockStatusEnum.SelfSignup:
            return "Self Signup";
          case LockStatusEnum.Supervised:
            return "Supervised";
          default:
            return "Unknown";
        }
    }
  }
}
