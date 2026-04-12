import { TestBed } from '@angular/core/testing';
import { CourseFormService, toHHMM } from './course-form.service';

describe('toHHMM', () => {
  it('strips seconds from HH:MM:SS', () => {
    expect(toHHMM('19:30:00')).toBe('19:30');
  });

  it('passes through already-short HH:MM', () => {
    expect(toHHMM('19:30')).toBe('19:30');
  });

  it('returns empty string for undefined', () => {
    expect(toHHMM(undefined)).toBe('');
  });
});

describe('CourseFormService.populate', () => {
  let service: CourseFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CourseFormService] });
    service = TestBed.inject(CourseFormService);
  });

  it('strips seconds from backend time fields before patching', () => {
    service.populate({
      title: 'T',
      danceStyle: 'BACHATA',
      level: 'BEGINNER',
      courseType: 'PARTNER',
      description: '',
      startDate: '2030-01-01',
      recurrenceType: 'WEEKLY',
      numberOfSessions: 8,
      startTime: '19:30:00',
      endTime: '21:00:00',
      location: 'Studio A',
      teachers: '',
      maxParticipants: 20,
      roleBalancingEnabled: false,
      roleBalanceThreshold: null,
      priceModel: 'FIXED_COURSE',
      price: 100,
    });

    expect(service.form.controls.schedule.controls.startTime.value).toBe('19:30');
    expect(service.form.controls.schedule.controls.endTime.value).toBe('21:00');
  });
});
