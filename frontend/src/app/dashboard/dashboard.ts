import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { AuthService } from '../shared/auth/auth.service';
import { PaymentService } from '../payments/payment.service';
import { StudentService } from '../students/student.service';
import { CourseService } from '../courses/course.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private auth = inject(AuthService);
  private paymentService = inject(PaymentService);
  private studentService = inject(StudentService);
  private courseService = inject(CourseService);
  private destroyRef = inject(DestroyRef);

  protected user = this.auth.user;
  protected hasSchool = computed(() => {
    const u = this.user();
    return u ? u.memberships.length > 0 : false;
  });

  protected loaded = signal(false);
  protected error = signal(false);

  protected activeStudentsCount = signal(0);
  protected activeCoursesCount = signal(0);
  protected totalRevenue = signal(0);
  protected openPaymentsCount = signal(0);
  protected openPaymentsTotal = signal(0);

  ngOnInit(): void {
    if (!this.hasSchool()) {
      this.loaded.set(true);
      return;
    }
    this.loadStats();
  }

  protected formatAmount(amount: number): string {
    return `CHF ${amount.toFixed(2)}`;
  }

  private loadStats(): void {
    forkJoin({
      payments: this.paymentService.getPayments(),
      students: this.studentService.getStudents(),
      courses: this.courseService.getCourses(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ payments, students, courses }) => {
        let openCount = 0;
        let openTotal = 0;
        let revenue = 0;
        for (const p of payments) {
          if (p.status === 'OPEN') {
            openCount++;
            openTotal += p.amount;
          } else if (p.status === 'COMPLETED') {
            revenue += p.amount;
          }
        }
        this.openPaymentsCount.set(openCount);
        this.openPaymentsTotal.set(openTotal);
        this.totalRevenue.set(revenue);
        this.activeStudentsCount.set(students.filter(s => s.activeCoursesCount > 0).length);
        this.activeCoursesCount.set(courses.length);
        this.loaded.set(true);
      },
      error: () => {
        this.error.set(true);
        this.loaded.set(true);
      },
    });
  }
}
