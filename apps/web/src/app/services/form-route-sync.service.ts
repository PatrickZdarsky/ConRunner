import {inject, Injectable} from "@angular/core";
import {AbstractControl} from "@angular/forms";
import {BehaviorSubject, filter, Observable, Subscription, take, withLatestFrom} from "rxjs";
import {Router} from "@angular/router";

interface formSyncConfig<> {
  subscriptions: Subscription[];
}

@Injectable({
  providedIn: "root"
})
export class FormRouteSyncService {

  private _forms = new Map<string, formSyncConfig>();
  private _router = inject(Router);

  constructor() { }

  public registerForm<TForm extends AbstractControl, TParams extends {[key: string]: string}>(
    key: string,
    form: TForm,
    syncToParams: (form: TForm) => TParams,
    syncFromParams: (params: TParams) => Observable<Parameters<TForm["patchValue"]>[0]>
  ): void {
    const existing = this._forms.get(key);
    if(existing) {
      existing.subscriptions.forEach(sub => sub.unsubscribe());
    }

    /* initially sync back on page load */
    const url = this._router.routerState.snapshot.root;
    const queryParams = url.queryParams as TParams;
    const patch = syncFromParams(queryParams);

    const hasEmitted$ = new BehaviorSubject(false);
    patch.pipe(
      take(1)
    ).subscribe(patchValue => {
      form.patchValue(patchValue, { emitEvent: true });
      hasEmitted$.next(true);
    });

    const formSub = form.valueChanges.pipe(
      withLatestFrom(hasEmitted$),
      filter(([, hasEmitted]) => hasEmitted)
    ).subscribe(() => {
      const params = syncToParams(form);
      this._router.navigate([], { queryParams: params, queryParamsHandling: "merge", replaceUrl: true });
    });

    this._forms.set(key, {
      subscriptions: [formSub]
    });
  }

  public unregisterForm(key: string): void {
    const existing = this._forms.get(key);
    if(existing) {
      existing.subscriptions.forEach(sub => sub.unsubscribe());
      this._forms.delete(key);
    }
  }
}
