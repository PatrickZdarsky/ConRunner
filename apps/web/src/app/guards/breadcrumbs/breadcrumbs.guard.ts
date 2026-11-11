import { CanActivateFn } from "@angular/router";
import {inject} from "@angular/core";
import {PageService} from "../../services/page/page.service";
import {Breadcrumb} from "../../util/breadcrumb";

export const breadcrumbsGuard: CanActivateFn = (route) => {

  /* set breadcrumbs only for leaf routes */
  if(route.children.length > 0) {return true;}

  const pageService = inject(PageService);

  /* reset calendar link */
  pageService.withCalendarLink(undefined);

  /* get breadcrumbs of route data */
  const routeData = route.data;
  const breadcrumbs = routeData["breadcrumbs"];

  /* if found, set as current breadcrumbs */
  if(breadcrumbs instanceof Breadcrumb) {
    pageService.withBreadcrumbs(breadcrumbs);
  } else {
    pageService.withBreadcrumbs(undefined);
  }

  return true;
};
