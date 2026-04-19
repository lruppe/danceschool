import { describe, it, expect } from 'vitest';
import {
  enrollmentStatusChipClass,
  formatDate,
  formatDayShort,
  formatDayFull,
  formatEnrollmentStatus,
  formatLevel,
  formatTime,
  formatWaitlistReason,
  levelChipClass,
  statusChipClass,
  waitlistReasonChipClass,
} from './format-utils';

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

describe('statusChipClass', () => {
  it('maps OPEN to success', () => {
    expect(statusChipClass('OPEN')).toBe('ds-chip-success');
  });

  it('maps RUNNING to primary', () => {
    expect(statusChipClass('RUNNING')).toBe('ds-chip-primary');
  });

  it('maps DRAFT and FINISHED to default', () => {
    expect(statusChipClass('DRAFT')).toBe('ds-chip-default');
    expect(statusChipClass('FINISHED')).toBe('ds-chip-default');
  });

  it('falls back to default for unknown status', () => {
    expect(statusChipClass('UNKNOWN')).toBe('ds-chip-default');
  });
});

describe('levelChipClass', () => {
  it('maps BEGINNER to info', () => {
    expect(levelChipClass('BEGINNER')).toBe('ds-chip-info');
  });

  it('maps INTERMEDIATE and MASTERCLASS to primary', () => {
    expect(levelChipClass('INTERMEDIATE')).toBe('ds-chip-primary');
    expect(levelChipClass('MASTERCLASS')).toBe('ds-chip-primary');
  });

  it('maps ADVANCED to success', () => {
    expect(levelChipClass('ADVANCED')).toBe('ds-chip-success');
  });

  it('maps STARTER and null to default', () => {
    expect(levelChipClass('STARTER')).toBe('ds-chip-default');
    expect(levelChipClass(null)).toBe('ds-chip-default');
  });
});

describe('formatLevel', () => {
  it('title-cases known levels', () => {
    expect(formatLevel('BEGINNER')).toBe('Beginner');
    expect(formatLevel('MASTERCLASS')).toBe('Masterclass');
  });

  it('returns "No level" for null', () => {
    expect(formatLevel(null)).toBe('No level');
  });
});

describe('waitlistReasonChipClass', () => {
  it('maps ROLE_IMBALANCE to info', () => {
    expect(waitlistReasonChipClass('ROLE_IMBALANCE')).toBe('ds-chip-info');
  });

  it('maps CAPACITY and null to default', () => {
    expect(waitlistReasonChipClass('CAPACITY')).toBe('ds-chip-default');
    expect(waitlistReasonChipClass(null)).toBe('ds-chip-default');
  });
});

describe('formatWaitlistReason', () => {
  it('formats CAPACITY and ROLE_IMBALANCE', () => {
    expect(formatWaitlistReason('CAPACITY')).toBe('Capacity');
    expect(formatWaitlistReason('ROLE_IMBALANCE')).toBe('Role imbalance');
  });

  it('returns em dash for null or unknown', () => {
    expect(formatWaitlistReason(null)).toBe('—');
    expect(formatWaitlistReason('SOMETHING_ELSE')).toBe('—');
  });
});
