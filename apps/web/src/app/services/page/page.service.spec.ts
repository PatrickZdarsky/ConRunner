import {TestBed} from "@angular/core/testing";
import {firstValueFrom} from "rxjs";
import {PageService} from "./page.service";

import {Breadcrumb} from "../../util/breadcrumb";

describe("PageService", () => {
  let service: PageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("withBreadcrumbs", () => {
    it("should store a clone of the given breadcrumb", async () => {
      const root = new Breadcrumb("Home", "/", undefined);
      service.withBreadcrumbs(root);

      const stored = await firstValueFrom(service.breadcrumbs$);

      expect(stored).toBeTruthy();
      expect(stored).not.toBe(root); // cloned
      expect(stored?.id).toBe(root.id); // same ID preserved
      expect(stored?.name).toBe("Home");
      expect(stored?.href).toBe("/");
    });

    it("should clear breadcrumbs if undefined is passed", async () => {
      const root = new Breadcrumb("Home", "/", undefined);
      service.withBreadcrumbs(root);
      service.withBreadcrumbs(undefined);

      expect(await firstValueFrom(service.breadcrumbs$)).toBeUndefined();
    });
  });

  describe("configureBreadcrumb", () => {
    let home: Breadcrumb;
    let events: Breadcrumb;
    let event: Breadcrumb;

    beforeEach(() => {
      home = new Breadcrumb("Home", "/", undefined);
      events = new Breadcrumb("Events", "events", home);
      event = new Breadcrumb("NamePlaceholder", "idPlaceholder", events);

      service.withBreadcrumbs(event);
    });

    it("should update name and href of the matched breadcrumb", async () => {
      service.configureBreadcrumb(events, "SomeEvent", "someId");

      const updated = await firstValueFrom(service.breadcrumbs$);
      const clonedChild = updated?.parent;

      expect(clonedChild?.name).toBe("SomeEvent");
      expect(clonedChild?.href).toBe("someId");
    });

    it("should not modify breadcrumbs if id does not match", async () => {
      const unrelated = new Breadcrumb("X", "x");

      const before = (await firstValueFrom(service.breadcrumbs$))?.clone();

      service.configureBreadcrumb(unrelated, "NoChange", "no-change");

      const after = await firstValueFrom(service.breadcrumbs$) as Breadcrumb;

      // unchanged because unrelated.id does not appear in chain
      expect(after.name).toBe(before?.name as string);
      expect(after.href).toBe(before?.href as string);
      expect(after.parent?.name).toBe(before?.parent?.name);
      expect(after.parent?.href).toBe(before?.parent?.href);
    });
  });
});
