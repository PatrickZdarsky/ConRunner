
/**
 * Returns the type of an element based on its tag name and role attributes.
 *
 * The function checks the tag name and, if necessary, the role attribute to determine
 * the element type (e.g., 'select', 'text', 'autocomplete', 'material-toggle', 'button', or 'unknown').
 *
 * @param {string} selector - The CSS selector for the element.
 * @returns {Cypress.Chainable<string>} The type of the element as a string.
 */
export function getElementType(selector: string) {
  return cy
    .get(selector)
    .should('exist')
    .invoke('prop', 'tagName')
    .then((tag) => {
      const tagName = String(tag).toLowerCase();

      return cy
        .get(selector)
        .invoke('attr', 'role')
        .then((roleRaw) => {
          const role = roleRaw ? String(roleRaw).toLowerCase() : null;

          if (tagName === 'select') return 'select';
          if (tagName === 'mat-select') return 'mat-select';
          if (tagName === 'input' && selector.includes('datepicker')) return 'datepicker';
          if (tagName === 'input' || tagName === 'textarea') {
            if (role === 'combobox') return 'autocomplete';
            return 'text';
          }
          if (tagName === 'button' && role === 'switch') return 'material-toggle';
          if (tagName === 'button') return 'button';
          if (tagName === 'input' && role === 'checkbox') return 'checkbox';
          if (tagName === 'mat-checkbox') return 'mat-checkbox';
          if (tagName === 'app-wysiwyg') return 'wysiwyg';
          if (tagName === 'span') return 'span';

          if (tagName === 'xsb-input-text') return 'xsb-text';
          if (tagName === 'xsb-input-toggle') return 'xsb-toggle';
          if (tagName === 'div') {
            return cy.get(selector).invoke('attr', 'id').then((id) => {
              if (!id) return 'unknown';
              return cy.get(selector)
                .find(`button[id^="${id}-option-"]`)
                .then(($buttons) => ($buttons.length ? 'xsb-multi-toggle' : 'unknown'));
            });
          }
          return 'unknown';
        });
    });
}
