import { Breadcrumb } from "./breadcrumb";

describe("Breadcrumb", () => {
  let home: Breadcrumb;
  let account: Breadcrumb;
  let events: Breadcrumb;
  let event: Breadcrumb;
  let shifts: Breadcrumb;
  let shift: Breadcrumb;

  beforeEach(() => {
    home = new Breadcrumb("Home", "/", undefined);
    account = new Breadcrumb("Account", "account", home);
    events = new Breadcrumb("Events", "/events", home);
    event = new Breadcrumb("EventName", "eventId", events);
    shifts = new Breadcrumb("Shifts", "/shifts", events);
    shift = new Breadcrumb("ShiftName", "shiftId", shifts);
  });

  it("should have unique id for each breadcrumb", () => {
    expect(home.id).not.toBe(events.id);
    expect(events.id).not.toBe(event.id);
    expect(home.id).not.toBe(event.id);
  });

  it("should allow updating name and href", () => {
    events.name = "Members";
    events.href = "members";

    expect(events.name).toBe("Members");
    expect(events.href).toBe("members");
  });

  describe("getPath", () => {
    it("should return the path from root to current breadcrumb", () => {
      const path = event.getPath();

      expect(path.length).toBe(3);
      expect(path[0]).toBe(home);
      expect(path[1]).toBe(events);
      expect(path[2]).toBe(event);
    });

    it("should return single element array if no parent", () => {
      const path = home.getPath();

      expect(path.length).toBe(1);
      expect(path[0]).toBe(home);
    });
  });

  describe("getFullHref", () => {
    it("should construct single href from parent-less breadcrumb", () => {
      const href = home.getFullHref();
      expect(href).toBe(home.href);
    });

    it("should construct concatenated href from breadcrumb with parent", () => {
      const href = account.getFullHref();
      expect(href).toBe(`${home.href}/${account.href}`);
    });

    it("should stop href concatenating if a parent returns a root indicator /", () => {
      const href = shift.getFullHref();
      expect(href).toBe(`${shifts.href}/${shift.href}`);
    });
  });

  describe("clone", () => {
    it("should create a deep clone with the same id", () => {
      const clone = event.clone();

      expect(clone).not.toBe(event);
      expect(clone.id).toBe(event.id);
      expect(clone.name).toBe(event.name);
      expect(clone.href).toBe(event.href);

      expect(clone.parent).not.toBe(event.parent);
      expect(clone.parent?.id).toBe(event.parent?.id);
      expect(clone.parent?.name).toBe(event.parent?.name);
    });

    it("modifying clone should not affect original", () => {
      const clone = event.clone();
      clone.name = "Changed Name";
      clone.href = "changed-href";

      expect(clone.name).toBe("Changed Name");
      expect(event.name).toBe("EventName"); // unchanged
    });
  });
});
