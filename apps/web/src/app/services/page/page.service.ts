import { Injectable } from "@angular/core";

import {Breadcrumb} from "../../util/breadcrumb";
import {BehaviorSubject} from "rxjs";

@Injectable({
  providedIn: "root"
})
export class PageService {

  private _breadcrumbs$ = new BehaviorSubject<Breadcrumb | undefined>(undefined);
  private _pageName$ = new BehaviorSubject<string | undefined>(undefined);
  private _calendarLink$ = new BehaviorSubject<string | undefined>(undefined);

  public get breadcrumbs$() {
    return this._breadcrumbs$.asObservable();
  }

  /**
   * Get the current page name
   */
  public get pageName$() {
    return this._pageName$.asObservable();
  }

  /**
   * Get the current calendar link
   */
  public get calendarLink$() {
    return this._calendarLink$.asObservable();
  }

  /**
   * Set the current breadcrumbs
   * @param breadcrumb
   */
  public withBreadcrumbs(breadcrumb?: Breadcrumb): PageService {
    this._breadcrumbs$.next(breadcrumb?.clone());
    this._pageName$.next(breadcrumb?.name);
    return this;
  }

  /**
   * Set the current calendar link
   * @param href
   */
  public withCalendarLink(href?: string): PageService {
    this._calendarLink$.next(href);
    return this;
  }

  /**
   * Configure one of the current breadcrumbs
   * @param breadcrumb the target breadcrumb to configure
   * @param name the new display name of the breadcrumb
   * @param href
   */
  public configureBreadcrumb(breadcrumb: Breadcrumb, name: string, href: string): PageService {
    const crumbs = this._breadcrumbs$.getValue();
    let traverse= crumbs;
    while (traverse !== undefined) {
      if(traverse.id === breadcrumb.id) {
        traverse.name = name;
        traverse.href = href;
        break;
      }
      traverse = traverse.parent;
    }

    this._breadcrumbs$.next(crumbs);
    return this;
  }

  public configurePageName(name: string): PageService {
    this._pageName$.next(name);
    return this;
  }
}
