import {Observable} from "rxjs";

export function tapLog<T>(name?: string): (source: Observable<T>) => Observable<T> {
  return (source: Observable<T>) =>
    new Observable<T>(observer => {
      source.subscribe({
        next: value => {
          console.log(name ?? "tapLog - next:", value);
          observer.next(value);
        },
        error: err => {
          console.error(name ?? "tapLog - error:", err);
          observer.error?.(err);
        },
        complete: () => {
          console.log(name ?? "tapLog - complete");
          observer.complete?.();
        }
      });
    });
}
