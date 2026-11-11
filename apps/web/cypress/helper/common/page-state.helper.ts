
export const PAGE_STATE = {
  OK: 'ok',
  ACCESS_DENIED: 'accessDenied',
  NOT_FOUND: 'notFound',
} as const;

export type PageState = (typeof PAGE_STATE)[keyof typeof PAGE_STATE];

const STATE_MAP: Record<string, PageState> = {
  ok: PAGE_STATE.OK,
  accessdenied: PAGE_STATE.ACCESS_DENIED,
  '403': PAGE_STATE.ACCESS_DENIED,
  notfound: PAGE_STATE.NOT_FOUND,
  '404': PAGE_STATE.NOT_FOUND,
};

export function parseState(raw: string): PageState {
  const k = (raw || '').trim().toLowerCase();
  const s = STATE_MAP[k];
  if (!s) throw new Error('Invalid PageState: ' + raw);
  return s;
}

export function expectPageState(expected: PageState, timeout: number): void {
  if (expected === PAGE_STATE.ACCESS_DENIED) {
    cy.location('pathname', { timeout }).should('include', '/403');
    return;
  }
  if (expected === PAGE_STATE.NOT_FOUND) {
    cy.location('pathname', { timeout }).should('include', '/404');
    return;
  }
  cy.location('pathname', { timeout }).should('not.match', /\/(403|404)(\/|$)/);
}
