import { ChangeDetectionStrategy, Component, inject, signal, OnDestroy } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CourseFormService } from './course-form.service';
import { CourseService } from '../course.service';
import {
  DANCE_STYLES, COURSE_LEVELS, COURSE_TYPES, RECURRENCE_TYPES,
  ROLE_BALANCING_MODES, PRICE_MODELS, COURSE_STATUSES,
} from '../../shared/course-constants';

interface StepDef {
  label: string;
}

@Component({
  selector: 'app-course-create',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatSlideToggleModule,
  ],
  providers: [CourseFormService],
  templateUrl: './course-create.html',
  styleUrl: './course-create.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseCreateComponent implements OnDestroy {
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  protected formService = inject(CourseFormService);
  private courseService = inject(CourseService);

  protected currentStep = signal(0);
  protected saving = signal(false);

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
  protected roleBalancingModes = ROLE_BALANCING_MODES;
  protected priceModels = PRICE_MODELS;
  protected courseStatuses = COURSE_STATUSES.filter(s => s.value === 'DRAFT' || s.value === 'ACTIVE');
  protected daysOfWeek = [
    { value: 'MONDAY', label: 'Monday' },
    { value: 'TUESDAY', label: 'Tuesday' },
    { value: 'WEDNESDAY', label: 'Wednesday' },
    { value: 'THURSDAY', label: 'Thursday' },
    { value: 'FRIDAY', label: 'Friday' },
    { value: 'SATURDAY', label: 'Saturday' },
    { value: 'SUNDAY', label: 'Sunday' },
  ];

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

  protected get isDraft(): boolean {
    return this.pricingGroup.controls.status.value === 'DRAFT';
  }

  protected label(items: { value: string; label: string }[], value: string): string {
    return items.find(i => i.value === value)?.label ?? value;
  }

  protected save(): void {
    if (this.saving()) return;
    this.saving.set(true);
    const dto = this.formService.toDto();
    this.courseService.createCourse(dto).subscribe({
      next: () => {
        this.snackBar.open('Course created successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/app/courses']);
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Failed to create course', 'Close', { duration: 3000 });
      },
    });
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
    // Only allow navigating to completed steps or the current step
    if (index <= this.currentStep()) {
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
