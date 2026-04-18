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

/** Map a course lifecycle status to its chip class. */
export function statusChipClass(status: string): string {
  switch (status) {
    case 'OPEN': return 'ds-chip-success';
    case 'RUNNING': return 'ds-chip-primary';
    case 'DRAFT': return 'ds-chip-default';
    case 'FINISHED': return 'ds-chip-default';
    default: return 'ds-chip-default';
  }
}

/** Map a dance level to its chip class. Null → neutral "No level" variant. */
export function levelChipClass(level: string | null): string {
  switch (level) {
    case 'BEGINNER': return 'ds-chip-info';
    case 'INTERMEDIATE': return 'ds-chip-primary';
    case 'ADVANCED': return 'ds-chip-success';
    case 'MASTERCLASS': return 'ds-chip-primary';
    case 'STARTER':
    default: return 'ds-chip-default';
  }
}

/** Display label for a dance level; null becomes "No level". */
export function formatLevel(level: string | null): string {
  if (!level) return 'No level';
  return level.charAt(0) + level.slice(1).toLowerCase();
}

/** Map an enrollment status to its chip class. */
export function enrollmentStatusChipClass(status: string): string {
  switch (status) {
    case 'CONFIRMED': return 'ds-chip-success';
    case 'PENDING_PAYMENT': return 'ds-chip-info';
    default: return 'ds-chip-default';
  }
}

/** Human-friendly label for an enrollment status (e.g. "PENDING_PAYMENT" → "PENDING PAYMENT"). */
export function formatEnrollmentStatus(status: string): string {
  return status.replace(/_/g, ' ');
}

/** Map a waitlist reason to its chip class. */
export function waitlistReasonChipClass(reason: string | null): string {
  switch (reason) {
    case 'ROLE_IMBALANCE': return 'ds-chip-info';
    case 'CAPACITY':       return 'ds-chip-default';
    default:               return 'ds-chip-default';
  }
}

/** Display label for a waitlist reason. */
export function formatWaitlistReason(reason: string | null): string {
  if (reason === 'CAPACITY') return 'Capacity';
  if (reason === 'ROLE_IMBALANCE') return 'Role imbalance';
  return '—';
}
