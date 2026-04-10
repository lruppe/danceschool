import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CourseFormService } from './course-form.service';
import { CourseService } from '../course.service';
import {
  DANCE_STYLES, COURSE_LEVELS, COURSE_TYPES, RECURRENCE_TYPES,
  ROLE_BALANCING_MODES, PRICE_MODELS, COURSE_STATUSES,
} from '../../shared/course-constants';
import { FieldHelpComponent } from '../../shared/field-help/field-help';

interface StepDef {
  label: string;
}

@Component({
  selector: 'app-course-create',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatSlideToggleModule, MatTooltipModule,
    FieldHelpComponent,
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
  protected roleBalancingModes = ROLE_BALANCING_MODES;
  protected priceModels = PRICE_MODELS;
  protected courseStatuses = COURSE_STATUSES.filter(s => s.value === 'DRAFT' || s.value === 'ACTIVE');
  private static readonly DAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

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

  protected get derivedDayOfWeek(): string {
    const startDate = this.scheduleGroup.controls.startDate.value;
    if (!startDate) return '';
    const date = new Date(startDate + 'T00:00:00');
    return CourseCreateComponent.DAY_NAMES[date.getDay()];
  }

  protected get derivedEndDate(): string {
    const startDate = this.scheduleGroup.controls.startDate.value;
    const sessions = this.scheduleGroup.controls.numberOfSessions.value;
    const recurrence = this.scheduleGroup.controls.recurrenceType.value;
    if (!startDate || !sessions || sessions < 1) return '';
    const date = new Date(startDate + 'T00:00:00');
    const intervalWeeks = recurrence === 'WEEKLY' ? 1 : 1;
    date.setDate(date.getDate() + (sessions - 1) * 7 * intervalWeeks);
    return date.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId.set(+id);
      this.loading.set(true);
      this.courseService.getCourse(+id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (data) => {
          this.formService.populate(data);
          this.currentStep.set(4); // Start on Review step
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load course', 'Close', { duration: 3000 });
          this.router.navigate(['/app/courses']);
        },
      });
    }
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
      this.snackBar.open(successMsg, 'Close', { duration: 3000 });
      this.router.navigate(['/app/courses']);
    };
    const onError = () => {
      this.saving.set(false);
      this.snackBar.open(errorMsg, 'Close', { duration: 3000 });
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
