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
      roleBalanceThreshold: null,
      priceModel: 'FIXED_COURSE',
      price: 100,
    });

    expect(service.form.controls.schedule.controls.startTime.value).toBe('19:30');
    expect(service.form.controls.schedule.controls.endTime.value).toBe('21:00');
  });
});

describe('CourseFormService.toDto', () => {
  let service: CourseFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CourseFormService] });
    service = TestBed.inject(CourseFormService);
  });

  function fillRequired(): void {
    service.form.patchValue({
      details: { title: 'T', danceStyle: 'BACHATA', level: 'BEGINNER', courseType: 'PARTNER' },
      schedule: {
        startDate: '2030-01-01', recurrenceType: 'WEEKLY', numberOfSessions: 8,
        startTime: '19:30', endTime: '20:45', location: 'Studio A',
      },
      registration: { maxParticipants: 20 },
      pricing: { priceModel: 'FIXED_COURSE', price: 100 },
    });
  }

  it('submits only roleBalanceThreshold — no roleBalancingEnabled field', () => {
    fillRequired();
    service.form.controls.registration.controls.roleBalanceThreshold.setValue(3);

    const dto = service.toDto();

    expect(dto).not.toHaveProperty('roleBalancingEnabled');
    expect(dto['roleBalanceThreshold']).toBe(3);
  });

  it('submits roleBalanceThreshold as null when balancing is off', () => {
    fillRequired();
    service.form.controls.registration.controls.roleBalanceThreshold.setValue(null);

    const dto = service.toDto();

    expect(dto['roleBalanceThreshold']).toBeNull();
    expect(dto).not.toHaveProperty('roleBalancingEnabled');
  });
});

describe('CourseFormService role-balancing toggle', () => {
  let service: CourseFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CourseFormService] });
    service = TestBed.inject(CourseFormService);
  });

  it('derives roleBalancingEnabled from the threshold value', () => {
    const control = service.form.controls.registration.controls.roleBalanceThreshold;

    control.setValue(null);
    expect(service.roleBalancingEnabled).toBe(false);

    control.setValue(3);
    expect(service.roleBalancingEnabled).toBe(true);
  });

  it('toggling on sets threshold to the default (3)', () => {
    service.form.controls.registration.controls.roleBalanceThreshold.setValue(null);
    service.setRoleBalancingEnabled(true);
    expect(service.form.controls.registration.controls.roleBalanceThreshold.value).toBe(3);
    expect(service.roleBalancingEnabled).toBe(true);
  });

  it('toggling off clears threshold to null', () => {
    service.form.controls.registration.controls.roleBalanceThreshold.setValue(5);
    service.setRoleBalancingEnabled(false);
    expect(service.form.controls.registration.controls.roleBalanceThreshold.value).toBeNull();
    expect(service.roleBalancingEnabled).toBe(false);
  });
});
