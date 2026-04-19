import { Injectable } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';

@Injectable()
export class CourseFormService {
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
      roleBalancingEnabled: new FormControl(false, { nonNullable: true }),
      roleBalanceThreshold: new FormControl<number | null>(3),
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
        roleBalancingEnabled: data['roleBalancingEnabled'] as boolean,
        roleBalanceThreshold: data['roleBalanceThreshold'] as number | null,
      },
      pricing: {
        priceModel: data['priceModel'] as string,
        price: data['price'] as number,
      },
    });
    // Mark as pristine after loading — form is not "dirty" until user edits
    this.form.markAsPristine();
  }

  toDto(): Record<string, unknown> {
    const v = this.form.getRawValue();
    return {
      ...v.details,
      ...v.schedule,
      ...v.registration,
      roleBalanceThreshold: v.registration.roleBalancingEnabled ? v.registration.roleBalanceThreshold : null,
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
