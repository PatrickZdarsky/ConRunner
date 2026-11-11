
const CONTEXT_KEY = 'featureContext';

type FeatureContextMap = Record<string, Record<string, string>>;

/** Stores a key-value pair for the current spec */
export function setFeatureContextValue(key: string, value: string): void {
  const spec: string = Cypress.spec.name;
  const raw: unknown = Cypress.env(CONTEXT_KEY);
  const all: FeatureContextMap = typeof raw === 'object' && raw !== null ? (raw as FeatureContextMap) : {};
  if (!all[spec]) {
    all[spec] = {};
  }
  all[spec][key] = value ?? '';
  Cypress.env(CONTEXT_KEY, all);
  Cypress.log({ name: 'FeatureContext.setValue', message: key + ' = ' + all[spec][key] });
}

/** Gets a stored value for the current spec */
export function getFeatureContextValue(key: string): string {
  const spec: string = Cypress.spec.name;
  const raw: unknown = Cypress.env(CONTEXT_KEY);
  const all: FeatureContextMap = typeof raw === 'object' && raw !== null ? (raw as FeatureContextMap) : {};
  const value: string = all[spec]?.[key] ?? '';
  Cypress.log({ name: 'FeatureContext.getValue', message: key + ' = ' + value });
  return value;
}

/** Clears only the context of the current spec */
export function clearFeatureContextForSpec(): void {
  const spec: string = Cypress.spec.name;
  const raw: unknown = Cypress.env(CONTEXT_KEY);
  const all: FeatureContextMap = typeof raw === 'object' && raw !== null ? (raw as FeatureContextMap) : {};
  if (all[spec]) {
    delete all[spec];
    Cypress.env(CONTEXT_KEY, all);
    Cypress.log({ name: 'FeatureContext.clear', message: spec + 'cleared' });
  }
}

/** Logs all stored values for the current spec */
export function logAllFeatureContextValues(): void {
  const spec: string = Cypress.spec.name;
  cy.then(() => {
    const raw: unknown = Cypress.env(CONTEXT_KEY);
    const all: FeatureContextMap = typeof raw === 'object' && raw !== null ? (raw as FeatureContextMap) : {};
    const entries: [string, string][] = Object.entries(all[spec] ?? {});
    const formatted: string = entries.map(([k, v]) => k + ' = ' + v).join(', ');
    cy.log('FeatureContext for ' + spec + ': {' + formatted + '}');
  });
}
