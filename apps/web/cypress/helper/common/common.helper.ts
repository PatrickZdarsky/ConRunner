
export const VALID_TOAST_TYPES = ['success', 'info', 'warning', 'error'] as const;

export type ToastType = (typeof VALID_TOAST_TYPES)[number];

/**
 * Validates and returns a typed toast type.
 *
 * @param toastType The toast type to validate as string.
 * @throws Error if toastType is invalid.
 * @returns The validated toast type.
 */
export function verifyToastType(toastType: string): ToastType {
  if (!VALID_TOAST_TYPES.includes(toastType as ToastType)) {
    throw new Error('Invalid toast type "' + toastType + '". Use: ' + VALID_TOAST_TYPES.join(', '));
  }
  return toastType as ToastType;
}
