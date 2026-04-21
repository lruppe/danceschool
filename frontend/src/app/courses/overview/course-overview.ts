import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass, TitleCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CourseDetail, CourseService } from '../course.service';
import { CourseSummaryComponent, CourseSummaryData } from '../shared/course-summary';
import { EnrollmentListItem, EnrollmentResponse, EnrollmentService } from '../enrollment.service';
import { enrollmentStatusChipClass, formatDate, formatDayFull, formatEnrollmentStatus, formatLevel, formatTime, formatWaitlistReason, levelChipClass, statusChipClass, waitlistReasonChipClass } from '../shared/format-utils';
import { extractErrorMessage } from '../../shared/error-utils';

interface EnrollmentTab {
  label: string;
  key: string;
}

const ENROLLMENT_TABS: EnrollmentTab[] = [
  { label: 'Enrolled', key: 'CONFIRMED' },
  { label: 'Waitlist', key: 'WAITLISTED' },
  { label: 'Approve', key: 'PENDING_APPROVAL' },
  { label: 'Open Payment', key: 'PENDING_PAYMENT' },
];

@Component({
  selector: 'app-course-overview',
  imports: [
    RouterLink, NgClass, TitleCasePipe,
    MatButtonModule, MatIconModule, MatTableModule, MatTabsModule, MatTooltipModule,
    CourseSummaryComponent,
  ],
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
  private enrollmentService = inject(EnrollmentService);

  protected course = signal<CourseDetail | null>(null);
  protected loading = signal(true);
  protected publishing = signal(false);
  protected deleting = signal(false);
  protected enrollments = signal<EnrollmentListItem[]>([]);
  protected activeTabIndex = signal(0);

  protected readonly tabs = ENROLLMENT_TABS;
  protected readonly enrollmentColumns = ['name', 'phone', 'role', 'status', 'enrolledAt', 'lastColumn'];

  protected enrolledList = computed(() =>
    this.enrollments().filter(e => e.status === 'CONFIRMED'));
  protected waitlistList = computed(() =>
    this.enrollments().filter(e => e.status === 'WAITLISTED'));
  protected approveList = computed(() =>
    this.enrollments().filter(e => e.status === 'PENDING_APPROVAL'));
  protected openPaymentList = computed(() =>
    this.enrollments().filter(e => e.status === 'PENDING_PAYMENT'));

  protected tabCounts = computed(() => [
    this.enrolledList().length,
    this.waitlistList().length,
    this.approveList().length,
    this.openPaymentList().length,
  ]);

  protected activeTabData = computed(() => {
    switch (this.activeTabIndex()) {
      case 0: return this.enrolledList();
      case 1: return this.waitlistList();
      case 2: return this.approveList();
      case 3: return this.openPaymentList();
      default: return [];
    }
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/app/courses']);
      return;
    }

    this.loadCourse(+id);
  }

  protected summaryData = computed<CourseSummaryData | null>(() => {
    const c = this.course();
    if (!c) return null;
    return {
      title: c.title,
      danceStyle: c.danceStyle,
      level: c.level,
      courseType: c.courseType,
      description: c.description,
      startDate: formatDate(c.startDate),
      dayOfWeek: formatDayFull(c.dayOfWeek),
      recurrenceType: c.recurrenceType,
      numberOfSessions: c.numberOfSessions,
      status: c.status,
      publishedAt: formatDate(c.publishedAt ?? ''),
      endDate: formatDate(c.endDate),
      startTime: formatTime(c.startTime),
      endTime: formatTime(c.endTime),
      location: c.location,
      teachers: c.teachers,
      maxParticipants: c.maxParticipants,
      roleBalanceThreshold: c.roleBalanceThreshold,
      priceModel: c.priceModel,
      price: c.price,
    };
  });

  protected statusChipClass = statusChipClass;
  protected levelChipClass = levelChipClass;
  protected formatLevel = formatLevel;
  protected enrollmentStatusChipClass = enrollmentStatusChipClass;
  protected formatEnrollmentStatus = formatEnrollmentStatus;
  protected waitlistReasonChipClass = waitlistReasonChipClass;
  protected formatWaitlistReason = formatWaitlistReason;

  protected selectTab(index: number): void {
    this.activeTabIndex.set(index);
  }

  protected formatEnrollmentDate(isoString: string | null): string {
    if (!isoString) return '—';
    const date = new Date(isoString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  protected formatRole(role: string | null): string {
    if (!role) return '—';
    return role === 'LEAD' ? 'Leader' : 'Follower';
  }

  protected onMarkPaid(enrollmentId: number): void {
    this.runEnrollmentAction(
      this.enrollmentService.markPaid(enrollmentId),
      () => 'Payment confirmed',
      'Failed to confirm payment',
    );
  }

  protected onApprove(enrollmentId: number): void {
    this.runEnrollmentAction(
      this.enrollmentService.approve(enrollmentId),
      (response) => response.status === 'WAITLISTED'
        ? 'Approved — course is full, moved to waitlist'
        : 'Enrollment approved',
      'Failed to approve enrollment',
      4000,
    );
  }

  protected onReject(enrollmentId: number): void {
    this.runEnrollmentAction(
      this.enrollmentService.reject(enrollmentId),
      () => 'Enrollment rejected',
      'Failed to reject enrollment',
    );
  }

  private runEnrollmentAction(
    op$: Observable<EnrollmentResponse>,
    successMessage: (response: EnrollmentResponse) => string,
    errorFallback: string,
    successDuration = 3000,
  ): void {
    op$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (response) => {
        this.snackBar.open(successMessage(response), 'Close', { duration: successDuration, panelClass: 'snackbar-success' });
        const courseId = this.course()?.id;
        if (courseId) this.loadEnrollments(courseId);
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(extractErrorMessage(err, errorFallback), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
      },
    });
  }

  protected onDelete(): void {
    const c = this.course();
    if (!c || c.status !== 'DRAFT') return;
    if (!confirm(`Delete "${c.title}"? This cannot be undone.`)) return;

    this.deleting.set(true);
    this.courseService.deleteCourse(c.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        this.snackBar.open('Course deleted', 'Close', { duration: 3000, panelClass: 'snackbar-success' });
        this.router.navigate(['/app/courses']);
      },
      error: (err: HttpErrorResponse) => {
        this.deleting.set(false);
        this.snackBar.open(extractErrorMessage(err, 'Failed to delete course'), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
      },
    });
  }

  protected onPublish(): void {
    const c = this.course();
    if (!c) return;

    this.publishing.set(true);
    this.courseService.publishCourse(c.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.course.set(updated);
        this.publishing.set(false);
        this.snackBar.open('Course published successfully', 'Close', { duration: 3000, panelClass: 'snackbar-success' });
      },
      error: (err: HttpErrorResponse) => {
        this.publishing.set(false);
        this.snackBar.open(extractErrorMessage(err, 'Failed to publish course'), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
      },
    });
  }

  protected onEdit(sectionIndex: number): void {
    const id = this.course()?.id;
    if (id) {
      this.router.navigate(['/app/courses', id, 'edit'], { queryParams: { step: sectionIndex } });
    }
  }

  private loadCourse(id: number): void {
    this.courseService.getCourse(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.course.set(data);
        this.loading.set(false);
        if (data.status !== 'DRAFT') {
          this.loadEnrollments(data.id);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(extractErrorMessage(err, 'Failed to load course'), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
        this.router.navigate(['/app/courses']);
      },
    });
  }

  private loadEnrollments(courseId: number): void {
    this.enrollmentService.getEnrollments(courseId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => this.enrollments.set(data),
      error: (err: HttpErrorResponse) => {
        this.enrollments.set([]);
        this.snackBar.open(extractErrorMessage(err, 'Failed to load enrollments'), 'Close', { duration: 5000, panelClass: 'snackbar-error' });
      },
    });
  }
}
