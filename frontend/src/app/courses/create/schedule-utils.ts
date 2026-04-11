import { RecurrenceType } from '../../shared/course-constants';

const DAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

export function deriveDayOfWeek(startDate: string): string {
  if (!startDate) return '';
  const date = new Date(startDate + 'T00:00:00');
  return DAY_NAMES[date.getDay()];
}

export function deriveEndDate(startDate: string, numberOfSessions: number | null, recurrenceType: RecurrenceType): string {
  if (!startDate || !numberOfSessions || numberOfSessions < 1) return '';
  const date = new Date(startDate + 'T00:00:00');
  const intervalWeeks = recurrenceType === 'WEEKLY' ? 1 : 1;
  date.setDate(date.getDate() + (numberOfSessions - 1) * 7 * intervalWeeks);
  return date.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
}
