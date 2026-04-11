import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass, TitleCasePipe } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { CourseDetail, CourseService } from '../course.service';
import { CourseSummaryComponent, CourseSummaryData } from '../shared/course-summary';
import { formatDayFull, formatTime } from '../shared/format-utils';
import { extractErrorMessage } from '../../shared/error-utils';

@Component({
  selector: 'app-course-overview',
  imports: [RouterLink, NgClass, TitleCasePipe, CourseSummaryComponent],
  templateUrl: './course-overview.html',
  styleUrl: './course-overview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseOverviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private courseService = inject(CourseService);

  protected course = signal<CourseDetail | null>(null);
  protected loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/app/courses']);
      return;
    }

    this.courseService.getCourse(+id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.course.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(extractErrorMessage(err, 'Failed to load course'), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
        this.router.navigate(['/app/courses']);
      },
    });
  }

  protected get summaryData(): CourseSummaryData | null {
    const c = this.course();
    if (!c) return null;
    return {
      title: c.title,
      danceStyle: c.danceStyle,
      level: c.level,
      courseType: c.courseType,
      description: c.description,
      startDate: c.startDate,
      dayOfWeek: formatDayFull(c.dayOfWeek),
      recurrenceType: c.recurrenceType,
      numberOfSessions: c.numberOfSessions,
      endDate: c.endDate,
      startTime: formatTime(c.startTime),
      endTime: formatTime(c.endTime),
      location: c.location,
      teachers: c.teachers,
      maxParticipants: c.maxParticipants,
      roleBalancingEnabled: c.roleBalancingEnabled,
      roleBalanceThreshold: c.roleBalanceThreshold,
      priceModel: c.priceModel,
      price: c.price,
      isPartnerCourse: c.courseType === 'PARTNER',
    };
  }

  protected statusChipClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'ds-chip-success';
      default: return 'ds-chip-default';
    }
  }

  protected onEdit(sectionIndex: number): void {
    const id = this.course()?.id;
    if (id) {
      this.router.navigate(['/app/courses', id, 'edit'], { queryParams: { step: sectionIndex } });
    }
  }
}
