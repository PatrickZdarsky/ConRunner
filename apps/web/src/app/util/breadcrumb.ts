export class Breadcrumb {
  private _id;

  constructor(private _name: string, private _href: string, private readonly _parent?: Breadcrumb) {
    this._id = Symbol(_name);
  }

  public get parent() {
    return this._parent;
  }

  public get name() {
    return this._name;
  }

  public get href() {
    return this._href;
  }

  public get id() {
    return this._id;
  }

  public set name(name: string) {
    this._name = name;
  }

  public set href(href: string) {
    this._href = href;
  }

  public getPath(): Breadcrumb[] {
    const path: Breadcrumb[] = [];

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    let current: Breadcrumb | undefined = this;
    while (current) {
      path.unshift(current);
      current = current.parent;
    }

    return path;
  }

  public getFullHref(): string {
    const path = this.getPath();
    const href: string[] = [];

    while (path.length > 0) {
      const segment = path.pop();
      if (segment) {
        href.unshift(segment.href);
        if (segment.href.startsWith("/")) {
          break;
        }
      }
    }

    return href.join("/");
  }

  /**
   * Clone this breadcrumb while preserving the id
   * @private
   */
  public clone(): Breadcrumb {
    const clone = new Breadcrumb(this._name, this._href, this._parent ? this._parent.clone() : undefined);
    clone._id = this._id;
    return clone;
  }
}
