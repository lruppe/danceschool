import { Injectable, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import type { CourseEditTier } from '../course.service';

export const DEFAULT_ROLE_BALANCE_THRESHOLD = 3;

/**
 * Fields locked in the RESTRICTED tier. Mirrors {@code CourseEditPolicy.LOCKED_IN_RESTRICTED}
 * on the backend. Kept as a client-side list so the template can disable controls without a
 * round-trip per field; the backend remains the canonical enforcement point.
 */
const LOCKED_IN_RESTRICTED: ReadonlySet<string> = new Set([
  'courseType',
  'price',
  'priceModel',
  'danceStyle',
  'level',
  'startDate',
  'endDate',
  'dayOfWeek',
  'startTime',
  'endTime',
  'numberOfSessions',
  'recurrenceType',
]);

@Injectable()
export class CourseFormService {
  private readonly _editTier = signal<CourseEditTier>('FULLY_EDITABLE');
  readonly editTier = this._editTier.asReadonly();

  readonly form = new FormGroup({
    details: new FormGroup({
      title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      danceStyle: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      level: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      courseType: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      description: new FormControl('', { nonNullable: true }),
    }),
    schedule: new FormGroup({
      startDate: new FormControl('', { nonNullable: true, validators: [Validators.required, futureDateValidator] }),
      recurrenceType: new FormControl('WEEKLY', { nonNullable: true, validators: [Validators.required] }),
      numberOfSessions: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      startTime: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      endTime: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      location: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      teachers: new FormControl('', { nonNullable: true }),
    }),
    registration: new FormGroup({
      maxParticipants: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      roleBalanceThreshold: new FormControl<number | null>(null),
    }),
    pricing: new FormGroup({
      priceModel: new FormControl('FIXED_COURSE', { nonNullable: true, validators: [Validators.required] }),
      price: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(0)] }),
    }),
  });

  private readonly stepGroups = [
    this.form.controls.details,
    this.form.controls.schedule,
    this.form.controls.registration,
    this.form.controls.pricing,
    // Review step has no form group — it displays all data
  ];

  isStepValid(index: number): boolean {
    const group = this.stepGroups[index];
    if (!group) return true; // Review step is always "valid"
    return group.valid;
  }

  markStepTouched(index: number): void {
    const group = this.stepGroups[index];
    if (group) {
      group.markAllAsTouched();
    }
  }

  populate(data: Record<string, unknown>): void {
    this.form.patchValue({
      details: {
        title: data['title'] as string,
        danceStyle: data['danceStyle'] as string,
        level: data['level'] as string,
        courseType: data['courseType'] as string,
        description: (data['description'] as string) ?? '',
      },
      schedule: {
        startDate: data['startDate'] as string,
        recurrenceType: data['recurrenceType'] as string,
        numberOfSessions: data['numberOfSessions'] as number,
        startTime: toHHMM(data['startTime'] as string | undefined),
        endTime: toHHMM(data['endTime'] as string | undefined),
        location: data['location'] as string,
        teachers: (data['teachers'] as string) ?? '',
      },
      registration: {
        maxParticipants: data['maxParticipants'] as number,
        roleBalanceThreshold: data['roleBalanceThreshold'] as number | null,
      },
      pricing: {
        priceModel: data['priceModel'] as string,
        price: data['price'] as number,
      },
    });
    const tier = (data['editTier'] as CourseEditTier | undefined) ?? 'FULLY_EDITABLE';
    this._editTier.set(tier);
    this.applyTierToControls(tier);
    // Mark as pristine after loading — form is not "dirty" until user edits
    this.form.markAsPristine();
  }

  isFieldLocked(field: string): boolean {
    const tier = this._editTier();
    if (tier === 'FULLY_EDITABLE') return false;
    if (tier === 'READ_ONLY') return true;
    return LOCKED_IN_RESTRICTED.has(field);
  }

  private applyTierToControls(_tier: CourseEditTier): void {
    // Disable every named form control whose field is locked under the current tier.
    // Reactive-forms `.disable()` is how Material input/select read disabled state.
    const named: Array<[string, AbstractControl]> = [
      ['title', this.form.controls.details.controls.title],
      ['danceStyle', this.form.controls.details.controls.danceStyle],
      ['level', this.form.controls.details.controls.level],
      ['courseType', this.form.controls.details.controls.courseType],
      ['description', this.form.controls.details.controls.description],
      ['startDate', this.form.controls.schedule.controls.startDate],
      ['recurrenceType', this.form.controls.schedule.controls.recurrenceType],
      ['numberOfSessions', this.form.controls.schedule.controls.numberOfSessions],
      ['startTime', this.form.controls.schedule.controls.startTime],
      ['endTime', this.form.controls.schedule.controls.endTime],
      ['location', this.form.controls.schedule.controls.location],
      ['teachers', this.form.controls.schedule.controls.teachers],
      ['maxParticipants', this.form.controls.registration.controls.maxParticipants],
      ['roleBalanceThreshold', this.form.controls.registration.controls.roleBalanceThreshold],
      ['priceModel', this.form.controls.pricing.controls.priceModel],
      ['price', this.form.controls.pricing.controls.price],
    ];
    for (const [name, control] of named) {
      if (this.isFieldLocked(name)) {
        control.disable({ emitEvent: false });
      } else {
        control.enable({ emitEvent: false });
      }
    }
  }

  get roleBalancingEnabled(): boolean {
    return this.form.controls.registration.controls.roleBalanceThreshold.value !== null;
  }

  setRoleBalancingEnabled(enabled: boolean): void {
    const control = this.form.controls.registration.controls.roleBalanceThreshold;
    control.setValue(enabled ? DEFAULT_ROLE_BALANCE_THRESHOLD : null);
    control.markAsDirty();
  }

  toDto(): Record<string, unknown> {
    const v = this.form.getRawValue();
    return {
      ...v.details,
      ...v.schedule,
      ...v.registration,
      priceModel: v.pricing.priceModel,
      price: v.pricing.price,
    };
  }

  isDirty(): boolean {
    return this.form.dirty;
  }

  clearFutureDateValidator(): void {
    this.form.controls.schedule.controls.startDate.removeValidators(futureDateValidator);
    this.form.controls.schedule.controls.startDate.updateValueAndValidity();
  }

  reset(): void {
    this.form.reset();
    this._editTier.set('FULLY_EDITABLE');
    this.applyTierToControls('FULLY_EDITABLE');
  }
}

export function toHHMM(value: string | undefined): string {
  return value ? value.slice(0, 5) : '';
}

export function futureDateValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const selected = new Date(value + 'T00:00:00');
  return selected > today ? null : { futureDate: true };
}
