import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass, TitleCasePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { StudentDanceLevel, StudentDetail, StudentService } from '../student.service';
import { formatLevel, levelChipClass } from '../../courses/shared/format-utils';
import { COURSE_LEVELS, CourseLevel, DANCE_STYLES, DanceStyle } from '../../shared/course-constants';
import { extractErrorMessage } from '../../shared/error-utils';

interface EditRow {
  danceStyle: DanceStyle | null;
  level: CourseLevel | null;
}

@Component({
  selector: 'app-student-detail',
  imports: [
    RouterLink, NgClass, TitleCasePipe,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatSelectModule,
  ],
  templateUrl: './student-detail.html',
  styleUrl: './student-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private studentService = inject(StudentService);
  private destroyRef = inject(DestroyRef);

  protected student = signal<StudentDetail | null>(null);
  protected loading = signal(true);
  protected saving = signal(false);

  protected original = signal<StudentDanceLevel[]>([]);
  protected edits = signal<EditRow[]>([]);

  protected danceStyles = DANCE_STYLES;
  protected levels = COURSE_LEVELS;
  protected levelChipClass = levelChipClass;
  protected formatLevel = formatLevel;

  protected canAddRow = computed(() => this.edits().length < DANCE_STYLES.length);

  protected hasValidChanges = computed(() => {
    const rows = this.edits();
    if (rows.some(r => r.danceStyle === null || r.level === null)) return false;
    return serialize(rows) !== serialize(this.original());
  });

  private studentId: number | null = null;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const parsed = idParam ? Number(idParam) : NaN;
    if (!Number.isFinite(parsed)) {
      this.router.navigate(['/app/students']);
      return;
    }
    this.studentId = parsed;

    this.studentService.getStudent(parsed).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.applyStudent(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(extractErrorMessage(err, 'Failed to load student'), 'Close', {
          duration: 5000, panelClass: 'snackbar-error',
        });
        this.router.navigate(['/app/students']);
      },
    });
  }

  protected availableStylesFor(index: number): { value: DanceStyle; label: string }[] {
    const used = new Set(
      this.edits()
        .map((r, i) => (i === index ? null : r.danceStyle))
        .filter((s): s is DanceStyle => s !== null),
    );
    return DANCE_STYLES.filter(s => !used.has(s.value));
  }

  protected onStyleChange(index: number, value: DanceStyle): void {
    this.edits.update(rows => rows.map((r, i) => (i === index ? { ...r, danceStyle: value } : r)));
  }

  protected onLevelChange(index: number, value: CourseLevel): void {
    this.edits.update(rows => rows.map((r, i) => (i === index ? { ...r, level: value } : r)));
  }

  protected addRow(): void {
    if (!this.canAddRow()) return;
    this.edits.update(rows => [...rows, { danceStyle: null, level: null }]);
  }

  protected onCancel(): void {
    this.edits.set(this.original().map(r => ({ ...r })));
  }

  protected onSave(): void {
    if (!this.hasValidChanges() || this.studentId === null) return;
    const payload = this.edits().map(r => ({
      danceStyle: r.danceStyle as DanceStyle,
      level: r.level as CourseLevel,
    }));

    this.saving.set(true);
    this.studentService.updateDanceLevels(this.studentId, { danceLevels: payload })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.applyStudent(result.student);
          this.saving.set(false);
          // Drop focus so the last-edited mat-select doesn't linger in its focused/primary-colored
          // state — combined with track $index on the list, the color can even bleed onto an
          // unrelated row after the server reorders.
          (document.activeElement as HTMLElement | null)?.blur();
          const message = result.autoConfirmedCount > 0
            ? `Saved. ${result.autoConfirmedCount} pending approval(s) auto-confirmed.`
            : 'Saved.';
          this.snackBar.open(message, 'Close', { duration: 3000 });
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          this.snackBar.open(extractErrorMessage(err, 'Failed to save dance levels'), 'Close', {
            duration: 5000, panelClass: 'snackbar-error',
          });
        },
      });
  }

  private applyStudent(data: StudentDetail): void {
    this.student.set(data);
    this.original.set(data.danceLevels.map(r => ({ ...r })));
    this.edits.set(data.danceLevels.map(r => ({ ...r })));
  }
}

function serialize(rows: { danceStyle: DanceStyle | null; level: CourseLevel | null }[]): string {
  return rows
    .map(r => `${r.danceStyle ?? ''}:${r.level ?? ''}`)
    .sort()
    .join('|');
}
