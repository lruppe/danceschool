import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass, TitleCasePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { StudentDetail, StudentService } from '../student.service';
import { formatLevel, levelChipClass } from '../../courses/shared/format-utils';
import { extractErrorMessage } from '../../shared/error-utils';

@Component({
  selector: 'app-student-detail',
  imports: [RouterLink, NgClass, TitleCasePipe, MatIconModule],
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

  protected levelChipClass = levelChipClass;
  protected formatLevel = formatLevel;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const parsed = id ? Number(id) : NaN;
    if (!Number.isFinite(parsed)) {
      this.router.navigate(['/app/students']);
      return;
    }

    this.studentService.getStudent(parsed).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.student.set(data);
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
}
