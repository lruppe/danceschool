import { ChangeDetectionStrategy, Component, inject, signal, OnDestroy } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CourseFormService } from './course-form.service';
import { CourseService } from '../course.service';
import { DANCE_STYLES, COURSE_LEVELS, COURSE_TYPES } from '../../shared/course-constants';

interface StepDef {
  label: string;
}

@Component({
  selector: 'app-course-create',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule,
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

  protected get detailsGroup() {
    return this.formService.form.controls.details;
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
