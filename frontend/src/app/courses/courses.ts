import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrencyPipe, TitleCasePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { CourseListItem, CourseService } from './course.service';

@Component({
  selector: 'app-courses',
  imports: [MatTableModule, MatIconModule, MatButtonModule, CurrencyPipe, TitleCasePipe],
  templateUrl: './courses.html',
  styleUrl: './courses.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoursesComponent implements OnInit {
  private courseService = inject(CourseService);
  private destroyRef = inject(DestroyRef);

  protected courses = signal<CourseListItem[]>([]);
  protected loaded = signal(false);
  protected error = signal(false);

  protected displayedColumns = [
    'title', 'danceStyle', 'level', 'schedule', 'enrollment', 'price', 'status',
  ];

  ngOnInit(): void {
    this.courseService.getCourses().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (courses) => {
        this.courses.set(courses);
        this.loaded.set(true);
      },
      error: () => {
        this.error.set(true);
        this.loaded.set(true);
      },
    });
  }

  protected formatDay(dayOfWeek: string): string {
    const days: Record<string, string> = {
      MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed',
      THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat', SUNDAY: 'Sun',
    };
    return days[dayOfWeek] ?? dayOfWeek;
  }

  protected formatTime(time: string): string {
    return time.substring(0, 5);
  }

  protected statusChipClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'chip-success';
      case 'FULL': return 'chip-info';
      case 'DRAFT': return 'chip-default';
      case 'INACTIVE': return 'chip-default';
      default: return 'chip-default';
    }
  }

  protected danceStyleChipClass(style: string): string {
    switch (style) {
      case 'BACHATA': return 'chip-primary';
      case 'SALSA': return 'chip-info';
      default: return 'chip-default';
    }
  }
}
