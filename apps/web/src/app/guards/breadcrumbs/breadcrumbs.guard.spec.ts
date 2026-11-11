import { TestBed } from "@angular/core/testing";
import {ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot} from "@angular/router";
import { breadcrumbsGuard } from "./breadcrumbs.guard";
import { PageService } from "../../services/page/page.service";

import {Breadcrumb} from "../../util/breadcrumb";

describe("breadcrumbsGuard", () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => breadcrumbsGuard(...guardParameters));

  let pageService: jasmine.SpyObj<PageService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: PageService,
          useValue: jasmine.createSpyObj("PageService", ["withBreadcrumbs"])
        }
      ]
    });

    pageService = TestBed.inject(PageService) as jasmine.SpyObj<PageService>;
  });

  it("should be created", () => {
    expect(executeGuard).toBeTruthy();
  });

  it("should return true and not set breadcrumbs when route.data.breadcrumbs not configured", () => {
    const result = executeGuard({
      children: [{}],
      data: {}
    } as ActivatedRouteSnapshot, {} as RouterStateSnapshot);

    expect(result).toBeTrue();
    expect(pageService.withBreadcrumbs).not.toHaveBeenCalled();
  });

  it("should set breadcrumbs when route.data.breadcrumbs is a Breadcrumb instance", () => {
    const breadcrumb = new Breadcrumb("Example", "/example");

    const result = executeGuard({
      children: [],
      data: { breadcrumbs: breadcrumb }
    } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot);

    expect(result).toBeTrue();
    expect(pageService.withBreadcrumbs).toHaveBeenCalledOnceWith(breadcrumb);
  });

  it("should clear breadcrumbs when route.data.breadcrumbs is not a Breadcrumb", () => {
    const result = executeGuard({
      children: [],
      data: {breadcrumbs: "not-a-breadcrumb"}
    } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot);

    expect(result).toBeTrue();
    expect(pageService.withBreadcrumbs).toHaveBeenCalledOnceWith(undefined);
  });
});
