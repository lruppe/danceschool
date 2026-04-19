import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';

import { HttpErrorResponse } from '@angular/common/http';
import { CourseFormService } from './course-form.service';
import { extractErrorMessage } from '../../shared/error-utils';
import { CourseService } from '../course.service';
import { CourseSummaryComponent, CourseSummaryData } from '../shared/course-summary';
import { formatDate } from '../shared/format-utils';
import { deriveDayOfWeek, deriveEndDate } from './schedule-utils';
import {
  DANCE_STYLES, COURSE_LEVELS, COURSE_TYPES, RECURRENCE_TYPES,
  PRICE_MODELS,
  RecurrenceType,
} from '../../shared/course-constants';

interface StepDef {
  label: string;
}

@Component({
  selector: 'app-course-create',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatSlideToggleModule,
    CourseSummaryComponent,
  ],
  providers: [CourseFormService],
  templateUrl: './course-create.html',
  styleUrl: './course-create.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseCreateComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  protected formService = inject(CourseFormService);
  private courseService = inject(CourseService);

  protected currentStep = signal(0);
  protected saving = signal(false);
  protected loading = signal(false);
  protected editId = signal<number | null>(null);

  protected get isEditMode(): boolean {
    return this.editId() !== null;
  }

  protected steps: StepDef[] = [
    { label: 'Details' },
    { label: 'Schedule' },
    { label: 'Registration' },
    { label: 'Pricing' },
    { label: 'Review' },
  ];

  protected danceStyles = DANCE_STYLES;
  protected levels = COURSE_LEVELS;
  protected courseTypes = COURSE_TYPES;
  protected recurrenceTypes = RECURRENCE_TYPES;
  protected priceModels = PRICE_MODELS;

  protected get detailsGroup() {
    return this.formService.form.controls.details;
  }

  protected get scheduleGroup() {
    return this.formService.form.controls.schedule;
  }

  protected get registrationGroup() {
    return this.formService.form.controls.registration;
  }

  protected get pricingGroup() {
    return this.formService.form.controls.pricing;
  }

  protected get isPartnerCourse(): boolean {
    return this.formService.form.controls.details.controls.courseType.value === 'PARTNER';
  }

  protected get reviewData(): CourseSummaryData {
    return {
      title: this.detailsGroup.controls.title.value,
      danceStyle: this.detailsGroup.controls.danceStyle.value,
      level: this.detailsGroup.controls.level.value,
      courseType: this.detailsGroup.controls.courseType.value,
      description: this.detailsGroup.controls.description.value || null,
      startDate: formatDate(this.scheduleGroup.controls.startDate.value),
      dayOfWeek: this.derivedDayOfWeek,
      recurrenceType: this.scheduleGroup.controls.recurrenceType.value,
      numberOfSessions: this.scheduleGroup.controls.numberOfSessions.value ?? 0,
      endDate: this.derivedEndDate || null,
      startTime: this.scheduleGroup.controls.startTime.value,
      endTime: this.scheduleGroup.controls.endTime.value,
      location: this.scheduleGroup.controls.location.value,
      teachers: this.scheduleGroup.controls.teachers.value || null,
      maxParticipants: this.registrationGroup.controls.maxParticipants.value ?? 0,
      roleBalanceThreshold: this.registrationGroup.controls.roleBalanceThreshold.value,
      priceModel: this.pricingGroup.controls.priceModel.value,
      price: this.pricingGroup.controls.price.value ?? 0,
    };
  }

  protected get derivedDayOfWeek(): string {
    return deriveDayOfWeek(this.scheduleGroup.controls.startDate.value);
  }

  protected get derivedEndDate(): string {
    return deriveEndDate(
      this.scheduleGroup.controls.startDate.value,
      this.scheduleGroup.controls.numberOfSessions.value,
      this.scheduleGroup.controls.recurrenceType.value as RecurrenceType,
    );
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId.set(+id);
      this.loading.set(true);
      this.formService.clearFutureDateValidator();
      this.courseService.getCourse(+id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (data) => {
          this.formService.populate(data as unknown as Record<string, unknown>);
          const stepParam = this.route.snapshot.queryParamMap.get('step');
          const step = stepParam !== null ? +stepParam : 4;
          this.currentStep.set(step >= 0 && step < this.steps.length ? step : 4);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.snackBar.open(extractErrorMessage(err, 'Failed to load course'), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
          this.router.navigate(['/app/courses']);
        },
      });
    }

    // Default role balancing ON when course type changes to PARTNER (create mode only)
    this.detailsGroup.controls.courseType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(type => {
        if (!this.isEditMode) {
          this.formService.setRoleBalancingEnabled(type === 'PARTNER');
        }
      });
  }

  protected get roleBalancingEnabled(): boolean {
    return this.formService.roleBalancingEnabled;
  }

  protected onRoleBalancingToggle(enabled: boolean): void {
    this.formService.setRoleBalancingEnabled(enabled);
  }

  protected label(items: { value: string; label: string }[], value: string): string {
    return items.find(i => i.value === value)?.label ?? value;
  }

  protected save(): void {
    if (this.saving()) return;
    this.saving.set(true);
    const dto = this.formService.toDto();
    const id = this.editId();
    const successMsg = id ? 'Course updated successfully' : 'Course created successfully';
    const errorMsg = id ? 'Failed to update course' : 'Failed to create course';
    const onSuccess = () => {
      this.snackBar.open(successMsg, 'Close', { duration: 3000, panelClass: 'snackbar-success' });
      this.router.navigate(id ? ['/app/courses', id] : ['/app/courses']);
    };
    const onError = (err: HttpErrorResponse) => {
      this.saving.set(false);
      this.snackBar.open(extractErrorMessage(err, errorMsg), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
    };
    if (id) {
      this.courseService.updateCourse(id, dto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: onSuccess, error: onError });
    } else {
      this.courseService.createCourse(dto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: onSuccess, error: onError });
    }
  }

  protected next(): void {
    if (!this.formService.isStepValid(this.currentStep())) {
      this.formService.markStepTouched(this.currentStep());
      return;
    }
    if (this.currentStep() < this.steps.length - 1) {
      this.currentStep.update(s => s + 1);
    }
  }

  protected back(): void {
    if (this.currentStep() > 0) {
      this.currentStep.update(s => s - 1);
    }
  }

  protected goToStep(index: number): void {
    // In edit mode, allow navigating to any step (all data is pre-populated)
    // In create mode, only allow completed steps or the current step
    if (this.isEditMode || index <= this.currentStep()) {
      this.currentStep.set(index);
    }
  }

  canDeactivate(): boolean {
    return !this.formService.isDirty() || this.saving();
  }

  ngOnDestroy(): void {
    this.formService.reset();
  }
}
