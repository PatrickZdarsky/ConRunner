import { Injectable } from "@angular/core";
import { Observable, Subject, map } from "rxjs";
import {dialogResult} from "../../components/dialog/dialog.component";

@Injectable({
  providedIn: "root"
})
export class DialogService {

  dialogStack: Array<{
    type: "danger" | "success" | "normal";
    title: string;
    action: string;
    message?: string;
    obs: Subject<dialogResult>;
    warnings?: string[];
  }> = [];

  get currentDialog() {
    return this.dialogStack[0];
  }

  confirmDanger(title: string, action: string, message?: string, warnings?: string[]) {
    const obs = new Subject<dialogResult>();
    this.dialogStack.push({ type: "danger", title, action, message, obs, warnings });
    return obs.pipe(
      map(result => result === "danger")
    ) as Observable<boolean>;
  }

  closeDialog(result: dialogResult) {
    const dialog = this.currentDialog;
    if (dialog !== undefined) {
      dialog.obs.next(result);
      dialog.obs.complete();
      this.dialogStack = this.dialogStack.filter(d => d !== dialog);
    }
  }
}
