
import { getFeatureContextValue } from '../common/feature-context.helper';

/**
 * Replaces date placeholders like YYYY, MM, DD, hh, mm with current date values.
 * Supports optional numeric offsets (e.g., MM+1, YYYY-1).
 *
 * Examples:
 *   "15.MM.YYYY 12:00"           → "15.04.2025 12:00"
 *   "DD.MM+1.YYYY-1 hh:mm"       → "16.05.2024 13:45"
 *   "report-YYYY-MM-DD"          → "report-2025-04-16"
 *   "newrole-MMDDYYYY-hhmm"       → "NewRole-04162025-1405"
 *   "nasimYYYY+1"                → "nasim2026"
 *   "MM+2-DD.YYYY"               → "06-16.2025"
 *
 * Placeholders:
 *   YYYY – full year
 *   MM   – month (01–12)
 *   DD   – day (01–31)
 *   hh   – hour (00–23)
 *   mm   – minute (00–59)
 *
 * Offset syntax:
 *   Add or subtract using +N or -N
 *   Examples: MM+1, YYYY-2, DD+10
 */
export function resolveDatePlaceholders(input: string): string {
  const now = new Date();

  const parts = {
    YYYY: now.getFullYear(),
    MM: now.getMonth() + 1,
    DD: now.getDate(),
    hh: now.getHours(),
    mm: now.getMinutes(),
  };
  type DateKey = keyof typeof parts;
  return input.replace(/(YYYY|MM|DD|hh|mm)([+-]\d+)?/g, (match, rawPart, offsetRaw) => {
    const part = rawPart as DateKey;
    const base = parts[part];

    const offset = offsetRaw ? Number(offsetRaw) : 0;
    return String(base + offset).padStart(2, '0');
  });
}

/**
 * Replaces all [KEY] placeholders in the string with values from FeatureContext.
 * Resolves nested placeholders recursively.
 *
 *  @param cell - The string containing one or more placeholders.
 *   @returns The input string with all placeholders resolved.
 */
export function resolvePlaceholders(cell: string): string {
  let str = resolveDatePlaceholders(cell);
  let last: string;
  do {
    last = str;
    str = str.replace(/\[([^[\]]+)\]/g, (full: string, key: string) => {
      const resolved: string = getFeatureContextValue(key);
      return resolved || full;
    });
  } while (str !== last);
  return str;
}
