import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { concat, EMPTY, Observable, of, switchMap, tap, toArray } from 'rxjs';
import { environment } from '../../environments/environment';
import { CourseDetail, CourseListItem, CourseService } from '../courses/course.service';
import { EnrollmentListItem, EnrollmentService, EnrollStudentRequest } from '../courses/enrollment.service';

const FIRST_NAMES = ['Anna', 'Marco', 'Laura', 'David', 'Sofia', 'Jan', 'Yuki', 'Elena', 'Thomas', 'Mia',
  'Lukas', 'Sarah', 'Alex', 'Nina', 'Felix', 'Julia', 'Max', 'Lena', 'Tobias', 'Clara'];
const LAST_NAMES = ['Mueller', 'Rossi', 'Weber', 'Kim', 'Martinez', 'de Vries', 'Tanaka', 'Fischer', 'Bauer', 'Schmidt',
  'Meier', 'Huber', 'Keller', 'Wagner', 'Braun', 'Steiner', 'Frey', 'Berger', 'Hess', 'Maurer'];

@Component({
  selector: 'app-dev-tools',
  imports: [
    LowerCasePipe, FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatSelectModule, MatTableModule, MatProgressSpinnerModule,
  ],
  templateUrl: './dev-tools.html',
  styleUrl: './dev-tools.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DevToolsComponent implements OnInit {
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private courseService = inject(CourseService);
  private enrollmentService = inject(EnrollmentService);

  protected courses = signal<CourseListItem[]>([]);
  protected selectedCourseId = signal<number | null>(null);
  protected selectedCourse = signal<CourseListItem | null>(null);
  protected courseDetail = signal<CourseDetail | null>(null);
  protected enrollments = signal<EnrollmentListItem[]>([]);
  protected filling = signal(false);
  protected adding = signal(false);
  protected paying = signal(false);
  protected danceRole = signal<'LEAD' | 'FOLLOW'>('LEAD');

  protected readonly enrollmentColumns = ['name', 'phone', 'role', 'status', 'enrolledAt'];

  ngOnInit(): void {
    this.loadCourses();
  }

  protected onCourseSelected(courseId: number): void {
    this.selectedCourseId.set(courseId);
    this.selectedCourse.set(this.courses().find(c => c.id === courseId) ?? null);
    this.courseDetail.set(null);
    this.courseService.getCourse(courseId).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(detail => this.courseDetail.set(detail));
    this.loadEnrollments(courseId);
  }

  protected isPartnerCourse(): boolean {
    return this.courseDetail()?.courseType === 'PARTNER';
  }

  protected onFillCourse(): void {
    const courseId = this.selectedCourseId();
    if (!courseId) return;

    this.filling.set(true);
    const course = this.selectedCourse();
    if (!course) return;

    const activeCount = this.enrollments().filter(
      e => e.status === 'CONFIRMED' || e.status === 'PENDING_PAYMENT',
    ).length;
    const spotsToFill = Math.max(0, course.maxParticipants - activeCount);

    if (spotsToFill === 0) {
      this.snackBar.open('Course is already full', 'Close', { duration: 3000 });
      this.filling.set(false);
      return;
    }

    // Create students and enroll them sequentially
    const isPartner = this.isPartnerCourse();
    const operations: Observable<unknown>[] = [];
    for (let i = 0; i < spotsToFill; i++) {
      const role: 'LEAD' | 'FOLLOW' = i % 2 === 0 ? 'LEAD' : 'FOLLOW';
      operations.push(
        this.createRandomStudent().pipe(
          switchMap(result => {
            const dto: EnrollStudentRequest = { studentId: result.id };
            if (isPartner) {
              dto.danceRole = role;
            }
            return this.enrollmentService.enrollStudent(courseId, dto);
          }),
        ),
      );
    }

    concat(...operations).pipe(
      toArray(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => {
        this.snackBar.open(`Enrolled ${spotsToFill} students`, 'Close', { duration: 3000, panelClass: 'snackbar-success' });
        this.filling.set(false);
        this.loadEnrollments(courseId);
      },
      error: () => {
        this.snackBar.open('Some enrollments failed', 'Close', { duration: 5000, panelClass: 'snackbar-error' });
        this.filling.set(false);
        this.loadEnrollments(courseId);
      },
    });
  }

  protected onAddStudent(): void {
    const courseId = this.selectedCourseId();
    if (!courseId) return;

    this.adding.set(true);

    this.createRandomStudent().pipe(
      switchMap(result => {
        const dto: EnrollStudentRequest = { studentId: result.id };
        if (this.isPartnerCourse()) {
          dto.danceRole = this.danceRole();
        }
        return this.enrollmentService.enrollStudent(courseId, dto);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (res) => {
        this.snackBar.open(`Student enrolled (${res.status})`, 'Close', { duration: 3000, panelClass: 'snackbar-success' });
        this.adding.set(false);
        this.loadEnrollments(courseId);
      },
      error: (err) => {
        const msg = err?.error?.detail || 'Failed to add student';
        this.snackBar.open(msg, 'Close', { duration: 5000, panelClass: 'snackbar-error' });
        this.adding.set(false);
      },
    });
  }

  protected onSimulatePayment(): void {
    const courseId = this.selectedCourseId();
    if (!courseId) return;

    const pendingPayments = this.enrollments().filter(e => e.status === 'PENDING_PAYMENT');
    if (pendingPayments.length === 0) {
      this.snackBar.open('No pending payments', 'Close', { duration: 3000 });
      return;
    }

    this.paying.set(true);
    const operations = pendingPayments.map(e => this.enrollmentService.markPaid(e.id));

    concat(...operations).pipe(
      toArray(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => {
        this.snackBar.open(`Confirmed ${pendingPayments.length} payments`, 'Close', { duration: 3000, panelClass: 'snackbar-success' });
        this.paying.set(false);
        this.loadEnrollments(courseId);
      },
      error: () => {
        this.snackBar.open('Some payments failed', 'Close', { duration: 5000, panelClass: 'snackbar-error' });
        this.paying.set(false);
        this.loadEnrollments(courseId);
      },
    });
  }

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }

  protected formatRole(role: string | null): string {
    if (!role) return '—';
    return role === 'LEAD' ? 'Leader' : 'Follower';
  }

  protected formatDate(isoString: string | null): string {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  private loadCourses(): void {
    const open$ = this.courseService.getCoursesByStatus('OPEN');
    const running$ = this.courseService.getCoursesByStatus('RUNNING');

    open$.pipe(
      switchMap(open => running$.pipe(
        tap(running => this.courses.set([...running, ...open])),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  private loadEnrollments(courseId: number): void {
    this.enrollmentService.getEnrollments(courseId).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: data => this.enrollments.set(data),
      error: () => this.enrollments.set([]),
    });
  }

  private createRandomStudent(): Observable<{ id: number }> {
    const firstName = FIRST_NAMES[Math.floor(Math.random() * FIRST_NAMES.length)];
    const lastName = LAST_NAMES[Math.floor(Math.random() * LAST_NAMES.length)];
    const name = `${firstName} ${lastName}`;
    const email = `${firstName.toLowerCase()}.${lastName.toLowerCase()}${Math.floor(Math.random() * 1000)}@test.com`;

    return this.http.post<{ id: number }>(`${environment.apiUrl}/api/students`, { name, email });
  }
}
