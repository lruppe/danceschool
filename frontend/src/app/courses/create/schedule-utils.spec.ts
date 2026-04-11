import { describe, it, expect } from 'vitest';
import { deriveDayOfWeek, deriveEndDate } from './schedule-utils';

describe('deriveDayOfWeek', () => {
  it('returns the correct day name for a Wednesday', () => {
    expect(deriveDayOfWeek('2026-04-15')).toBe('Wednesday');
  });

  it('returns the correct day name for a Monday', () => {
    expect(deriveDayOfWeek('2026-04-06')).toBe('Monday');
  });

  it('returns empty string for empty input', () => {
    expect(deriveDayOfWeek('')).toBe('');
  });
});

describe('deriveEndDate', () => {
  it('calculates end date for 10 weekly sessions starting April 15', () => {
    const result = deriveEndDate('2026-04-15', 10, 'WEEKLY');
    expect(result).toBe('17.06.2026');
  });

  it('returns the start date itself for 1 session', () => {
    const result = deriveEndDate('2026-04-15', 1, 'WEEKLY');
    expect(result).toBe('15.04.2026');
  });

  it('returns empty string when startDate is missing', () => {
    expect(deriveEndDate('', 10, 'WEEKLY')).toBe('');
  });

  it('returns empty string when sessions is null', () => {
    expect(deriveEndDate('2026-04-15', null, 'WEEKLY')).toBe('');
  });

  it('returns empty string when sessions is 0', () => {
    expect(deriveEndDate('2026-04-15', 0, 'WEEKLY')).toBe('');
  });
});
