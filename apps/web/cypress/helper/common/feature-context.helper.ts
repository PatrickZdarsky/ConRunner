const contextBySpec = new Map<string, Map<string, string>>();

/** Stores a key-value pair for the current spec */
export function setFeatureContextValue(key: string, value: string): void {
  const spec = Cypress.spec.name;
  const context = contextBySpec.get(spec) ?? new Map<string, string>();
  context.set(key, value ?? '');
  contextBySpec.set(spec, context);
  Cypress.log({ name: 'FeatureContext.setValue', message: key + ' = ' + context.get(key) });
}

/** Gets a stored value for the current spec */
export function getFeatureContextValue(key: string): string {
  const value = contextBySpec.get(Cypress.spec.name)?.get(key) ?? '';
  Cypress.log({ name: 'FeatureContext.getValue', message: key + ' = ' + value });
  return value;
}

/** Clears only the context of the current spec */
export function clearFeatureContextForSpec(): void {
  const spec = Cypress.spec.name;
  if (contextBySpec.delete(spec)) {
    Cypress.log({ name: 'FeatureContext.clear', message: spec + ' cleared' });
  }
}

/** Logs all stored values for the current spec */
export function logAllFeatureContextValues(): void {
  const spec = Cypress.spec.name;
  cy.then(() => {
    const entries = Array.from(contextBySpec.get(spec) ?? []);
    const formatted = entries.map(([key, value]) => key + ' = ' + value).join(', ');
    cy.log('FeatureContext for ' + spec + ': {' + formatted + '}');
  });
}
