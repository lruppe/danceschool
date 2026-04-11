const DAY_ABBREVIATIONS: Record<string, string> = {
  MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed',
  THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat', SUNDAY: 'Sun',
};

/** Abbreviate a backend DayOfWeek enum value (e.g. "FRIDAY" → "Fri"). */
export function formatDayShort(dayOfWeek: string): string {
  return DAY_ABBREVIATIONS[dayOfWeek] ?? dayOfWeek;
}

/** Title-case a backend DayOfWeek enum value (e.g. "FRIDAY" → "Friday"). */
export function formatDayFull(dayOfWeek: string): string {
  return dayOfWeek.charAt(0).toUpperCase() + dayOfWeek.slice(1).toLowerCase();
}

/** Strip seconds from a backend time string (e.g. "19:30:00" → "19:30"). */
export function formatTime(time: string): string {
  return time.substring(0, 5);
}

/** Format an ISO date string to European format (e.g. "2026-04-11" → "11.04.2026"). */
export function formatDate(isoDate: string): string {
  if (!isoDate) return '';
  const [year, month, day] = isoDate.split('-');
  return `${day}.${month}.${year}`;
}
