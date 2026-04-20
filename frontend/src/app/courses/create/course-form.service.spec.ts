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

describe('CourseFormService.isFieldLocked (edit-tier signal)', () => {
  let service: CourseFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CourseFormService] });
    service = TestBed.inject(CourseFormService);
  });

  function populateWith(editTier: string | undefined): void {
    service.populate({
      title: 'T',
      danceStyle: 'BACHATA',
      level: 'BEGINNER',
      courseType: 'PARTNER',
      description: '',
      startDate: '2030-01-01',
      recurrenceType: 'WEEKLY',
      numberOfSessions: 8,
      startTime: '19:30',
      endTime: '20:45',
      location: 'Studio A',
      teachers: '',
      maxParticipants: 20,
      roleBalanceThreshold: null,
      priceModel: 'FIXED_COURSE',
      price: 100,
      editTier,
    });
  }

  it('FULLY_EDITABLE tier: no field is locked', () => {
    populateWith('FULLY_EDITABLE');
    expect(service.isFieldLocked('title')).toBe(false);
    expect(service.isFieldLocked('danceStyle')).toBe(false);
    expect(service.isFieldLocked('price')).toBe(false);
    expect(service.isFieldLocked('startTime')).toBe(false);
  });

  it('RESTRICTED tier: locks the locked-field list, leaves cosmetic fields editable', () => {
    populateWith('RESTRICTED');
    expect(service.isFieldLocked('courseType')).toBe(true);
    expect(service.isFieldLocked('price')).toBe(true);
    expect(service.isFieldLocked('danceStyle')).toBe(true);
    expect(service.isFieldLocked('startDate')).toBe(true);
    expect(service.isFieldLocked('numberOfSessions')).toBe(true);

    expect(service.isFieldLocked('title')).toBe(false);
    expect(service.isFieldLocked('description')).toBe(false);
    expect(service.isFieldLocked('teachers')).toBe(false);
    expect(service.isFieldLocked('location')).toBe(false);
    expect(service.isFieldLocked('maxParticipants')).toBe(false);
    expect(service.isFieldLocked('roleBalanceThreshold')).toBe(false);
  });

  it('READ_ONLY tier: every field is locked', () => {
    populateWith('READ_ONLY');
    expect(service.isFieldLocked('title')).toBe(true);
    expect(service.isFieldLocked('maxParticipants')).toBe(true);
    expect(service.isFieldLocked('danceStyle')).toBe(true);
  });

  it('missing editTier defaults to FULLY_EDITABLE', () => {
    populateWith(undefined);
    expect(service.isFieldLocked('danceStyle')).toBe(false);
  });

  it('RESTRICTED tier disables the locked form controls', () => {
    populateWith('RESTRICTED');
    expect(service.form.controls.details.controls.danceStyle.disabled).toBe(true);
    expect(service.form.controls.pricing.controls.price.disabled).toBe(true);
    expect(service.form.controls.schedule.controls.startTime.disabled).toBe(true);
    expect(service.form.controls.details.controls.title.disabled).toBe(false);
    expect(service.form.controls.schedule.controls.location.disabled).toBe(false);
  });

  it('READ_ONLY tier disables every form control', () => {
    populateWith('READ_ONLY');
    expect(service.form.controls.details.controls.title.disabled).toBe(true);
    expect(service.form.controls.schedule.controls.location.disabled).toBe(true);
    expect(service.form.controls.registration.controls.maxParticipants.disabled).toBe(true);
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
