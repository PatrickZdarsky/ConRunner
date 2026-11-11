import {inject, Injectable} from "@angular/core";
import {ToastrService} from "ngx-toastr";
import {Observable} from "rxjs";
import {mapValue} from "../../util/value-maps";

@Injectable({
  providedIn: "root"
})
export class ToastService {
  private readonly _toastr = inject(ToastrService);

  constructor() { }

  public showNotification(title: string, message: string, url?: string, timeout?: number) {
    this._toastr.info(message,title, {
      timeOut: timeout ?? 5000,
      payload: {
        notification: true,
        url
      }
    });
  }

  public showInfo(title: string, message: string, timeout?: number) {
    this._toastr.info(message,title, {
      timeOut: timeout ?? 5000
    });
  }

  public showError(title: string, message: string, timeout?: number) {
    this._toastr.error(message,title, {
      timeOut: timeout ?? 5000
    });
  }

  public showSuccess(title: string, message: string, timeout?: number) {
    this._toastr.success(message,title, {
      timeOut: timeout ?? 5000
    });
  }

  public tapSuccess<T>(successName: string, successDetail?: (value: T) => string): (source: Observable<T>) => Observable<T> {
    return (source: Observable<T>) =>
      new Observable<T>(observer => {
        source.subscribe({
          next: value => {
            const detail = successDetail ? successDetail(value) : "";
            this.showSuccess(successName, detail);
            observer.next(value);
          },
          error: err => observer.error?.(err),
          complete: () => observer.complete?.()
        });
      });
  }

  public tapNotification<T>(notificationName: string, notificationDetail?: (value: T) => string): (source: Observable<T>) => Observable<T> {
    return (source: Observable<T>) =>
      new Observable<T>(observer => {
        source.subscribe({
          next: value => {
            const detail = notificationDetail ? notificationDetail(value) : "";
            this.showNotification(notificationName, detail);
            observer.next(value);
          },
          error: err => observer.error?.(err),
          complete: () => observer.complete?.()
        });
      });
  }

  public tapInfo<T>(infoName: string, infoDetail?: (value: T) => string): (source: Observable<T>) => Observable<T> {
    return (source: Observable<T>) =>
      new Observable<T>(observer => {
        source.subscribe({
          next: value => {
            const detail = infoDetail ? infoDetail(value) : "";
            this.showInfo(infoName, detail);
            observer.next(value);
          },
          error: err => observer.error?.(err),
          complete: () => observer.complete?.()
        });
      });
  }

  public tapError<T>(errorName: string, errorDetail?: (value: unknown) => string): (source: Observable<T>) => Observable<T> {
    return (source: Observable<T>) =>
      new Observable<T>(observer => {
        source.subscribe({
          next: value => {
            observer.next(value);
          },
          error: err => {
            const detail = errorDetail ? errorDetail(err) : "";
            this.showError(errorName, detail);
            observer.error?.(err);
          },
          complete: () => observer.complete?.()
        });
      });
  }

  public tapCreating<T>(itemType: string, itemName?: (value: T) => string): (source: Observable<T>) => Observable<T> {
    const success = this.tapSuccess<T>(
      `${itemType} Created`,
      itemName ? (value: T) => `${itemType} "${itemName(value)}" has been created.` : undefined
    );
    const error = this.tapError<T>(
      `Error Creating ${itemType}`,
      (err: unknown) => mapValue.apiErrorToMessage(err)
    );
    return (source: Observable<T>) => source.pipe(success, error);
  }

  public tapSaving<T>(itemType: string, itemName?: (value: T) => string): (source: Observable<T>) => Observable<T> {
    const success = this.tapSuccess<T>(
      `${itemType} Saved`,
      itemName ? (value: T) => `${itemType} "${itemName(value)}" has been saved.` : undefined
    );
    const error = this.tapError<T>(
      `Error Saving ${itemType}`,
      (err: unknown) => mapValue.apiErrorToMessage(err)
    );
    return (source: Observable<T>) => source.pipe(success, error);
  }

  public tapDeleting<T>(itemType: string, itemName?: (value: T) => string): (source: Observable<T>) => Observable<T> {
    const success = this.tapSuccess<T>(
      `${itemType} Deleted`,
      itemName ? (value: T) => `${itemType} "${itemName(value)}" has been deleted.` : undefined
    );
    const error = this.tapError<T>(
      `Error Deleting ${itemType}`,
      (err: unknown) => mapValue.apiErrorToMessage(err)
    );
    return (source: Observable<T>) => source.pipe(success, error);
  }
}
