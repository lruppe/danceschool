import { describe, it, expect } from 'vitest';
import { enrollmentStatusChipClass, formatDate, formatDayShort, formatDayFull, formatEnrollmentStatus, formatTime } from './format-utils';

describe('formatDate', () => {
  it('formats ISO date to European format', () => {
    expect(formatDate('2026-04-11')).toBe('11.04.2026');
  });

  it('preserves leading zeros', () => {
    expect(formatDate('2026-01-05')).toBe('05.01.2026');
  });

  it('returns empty string for empty input', () => {
    expect(formatDate('')).toBe('');
  });
});

describe('formatDayShort', () => {
  it('abbreviates day of week', () => {
    expect(formatDayShort('FRIDAY')).toBe('Fri');
  });

  it('returns input for unknown day', () => {
    expect(formatDayShort('UNKNOWN')).toBe('UNKNOWN');
  });
});

describe('formatDayFull', () => {
  it('title-cases day of week', () => {
    expect(formatDayFull('FRIDAY')).toBe('Friday');
  });
});

describe('formatTime', () => {
  it('strips seconds from time', () => {
    expect(formatTime('19:30:00')).toBe('19:30');
  });
});

describe('enrollmentStatusChipClass', () => {
  it('maps CONFIRMED to success', () => {
    expect(enrollmentStatusChipClass('CONFIRMED')).toBe('ds-chip-success');
  });

  it('maps PENDING_PAYMENT to info', () => {
    expect(enrollmentStatusChipClass('PENDING_PAYMENT')).toBe('ds-chip-info');
  });

  it('maps other statuses to default', () => {
    expect(enrollmentStatusChipClass('WAITLISTED')).toBe('ds-chip-default');
    expect(enrollmentStatusChipClass('PENDING_APPROVAL')).toBe('ds-chip-default');
    expect(enrollmentStatusChipClass('REJECTED')).toBe('ds-chip-default');
  });
});

describe('formatEnrollmentStatus', () => {
  it('replaces underscores with spaces', () => {
    expect(formatEnrollmentStatus('PENDING_PAYMENT')).toBe('PENDING PAYMENT');
    expect(formatEnrollmentStatus('PENDING_APPROVAL')).toBe('PENDING APPROVAL');
  });

  it('leaves single-word statuses unchanged', () => {
    expect(formatEnrollmentStatus('CONFIRMED')).toBe('CONFIRMED');
  });
});
