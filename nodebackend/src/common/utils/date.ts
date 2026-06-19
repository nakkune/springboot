/**
 * Safely parses a date string, ignoring leading plus signs and cleaning up typos/formatting issues.
 * Returns a valid Date object, or falls back to a fallback date (or current date).
 */
export function parseSafeDate(dateStr: any, fallbackDate = new Date()): Date {
  if (!dateStr) return fallbackDate;
  if (dateStr instanceof Date) {
    return isNaN(dateStr.getTime()) ? fallbackDate : dateStr;
  }

  let cleaned = String(dateStr).trim();
  // Strip leading plus/minus
  if (cleaned.startsWith('+') || cleaned.startsWith('-')) {
    cleaned = cleaned.substring(1);
  }

  // Handle YYYYMMDD -> YYYY-MM-DD
  if (/^\d{8}$/.test(cleaned)) {
    cleaned = `${cleaned.substring(0, 4)}-${cleaned.substring(4, 6)}-${cleaned.substring(6, 8)}`;
  }

  const parsed = new Date(cleaned);
  if (!isNaN(parsed.getTime())) {
    return parsed;
  }

  // Fallback: extract digits and reconstruct YYYY-MM-DD
  const digits = cleaned.replace(/\D/g, '');
  if (digits.length >= 8) {
    const y = digits.substring(0, 4);
    const m = digits.substring(4, 6);
    const d = digits.substring(6, 8);
    const rebuilt = new Date(`${y}-${m}-${d}`);
    if (!isNaN(rebuilt.getTime())) {
      return rebuilt;
    }
  }

  return fallbackDate;
}
