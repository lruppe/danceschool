import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

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
      startDate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      recurrenceType: new FormControl('WEEKLY', { nonNullable: true, validators: [Validators.required] }),
      dayOfWeek: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      numberOfSessions: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      startTime: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      endTime: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      location: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      teachers: new FormControl('', { nonNullable: true }),
    }),
    registration: new FormGroup({
      maxParticipants: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      waitingListEnabled: new FormControl(false, { nonNullable: true }),
      requireRoleSelection: new FormControl(false, { nonNullable: true }),
      roleBalancingMode: new FormControl('', { nonNullable: true }),
      roleBalanceThreshold: new FormControl<number | null>(null),
    }),
    pricing: new FormGroup({}),
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

  isDirty(): boolean {
    return this.form.dirty;
  }

  reset(): void {
    this.form.reset();
  }
}
