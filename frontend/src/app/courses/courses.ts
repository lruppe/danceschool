import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit, ViewChild, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrencyPipe, NgClass, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CourseListItem, CourseService } from './course.service';
import { formatDayShort, formatTime as stripSeconds } from './shared/format-utils';
import { DANCE_STYLES, COURSE_LEVELS } from '../shared/course-constants';

interface CourseFilter {
  text: string;
  danceStyle: string;
  level: string;
}

const DAY_ORDER: Record<string, number> = {
  MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3,
  THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 7,
};

@Component({
  selector: 'app-courses',
  imports: [
    MatTableModule, MatSortModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    FormsModule, RouterLink, CurrencyPipe, TitleCasePipe, NgClass,
  ],
  templateUrl: './courses.html',
  styleUrl: './courses.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoursesComponent implements OnInit {
  private courseService = inject(CourseService);
  private destroyRef = inject(DestroyRef);

  protected dataSource = new MatTableDataSource<CourseListItem>([]);
  protected totalCount = signal(0);
  protected loaded = signal(false);
  protected error = signal(false);

  protected searchText = '';
  protected selectedDanceStyle = '';
  protected selectedLevel = '';

  protected danceStyles = DANCE_STYLES.map(d => d.value);
  protected levels = COURSE_LEVELS.map(l => l.value);

  protected filteredCount = computed(() => this.dataSource.filteredData.length);

  protected displayedColumns = [
    'title', 'danceStyle', 'level', 'schedule', 'enrollment', 'price', 'status',
  ];

  @ViewChild(MatSort) set sort(sort: MatSort) {
    if (sort) {
      this.dataSource.sort = sort;
    }
  }

  ngOnInit(): void {
    this.dataSource.filterPredicate = this.createFilterPredicate();
    this.dataSource.sortingDataAccessor = (course, column) => {
      if (column === 'schedule') {
        return DAY_ORDER[course.dayOfWeek] ?? 0;
      }
      return (course as unknown as Record<string, string | number>)[column];
    };

    this.courseService.getCourses().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (courses) => {
        this.dataSource.data = courses;
        this.totalCount.set(courses.length);
        this.loaded.set(true);
      },
      error: () => {
        this.error.set(true);
        this.loaded.set(true);
      },
    });
  }

  protected applyFilter(): void {
    const filter: CourseFilter = {
      text: this.searchText.trim().toLowerCase(),
      danceStyle: this.selectedDanceStyle,
      level: this.selectedLevel,
    };
    // MatTableDataSource.filter is a string — serialize the filter object
    this.dataSource.filter = JSON.stringify(filter);
  }

  private createFilterPredicate(): (data: CourseListItem, filter: string) => boolean {
    return (data: CourseListItem, filter: string): boolean => {
      const f: CourseFilter = JSON.parse(filter);

      // Dance style dropdown filter
      if (f.danceStyle && data.danceStyle !== f.danceStyle) {
        return false;
      }

      // Level dropdown filter
      if (f.level && data.level !== f.level) {
        return false;
      }

      // Free-text search across text columns
      if (f.text) {
        const searchable = [
          data.title,
          data.danceStyle,
          data.level,
          data.status,
          data.dayOfWeek,
        ].join(' ').toLowerCase();
        if (!searchable.includes(f.text)) {
          return false;
        }
      }

      return true;
    };
  }

  protected formatDay(dayOfWeek: string): string {
    return formatDayShort(dayOfWeek);
  }

  protected formatTime(time: string): string {
    return stripSeconds(time);
  }

  protected sessionDuration(startTime: string, endTime: string): number {
    const [startH, startM] = startTime.split(':').map(Number);
    const [endH, endM] = endTime.split(':').map(Number);
    return (endH * 60 + endM) - (startH * 60 + startM);
  }

  protected statusChipClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'ds-chip-success';
      case 'FULL': return 'ds-chip-default';
      case 'DRAFT': return 'ds-chip-default';
      case 'INACTIVE': return 'ds-chip-default';
      default: return 'ds-chip-default';
    }
  }

  protected danceStyleChipClass(style: string): string {
    switch (style) {
      case 'BACHATA': return 'ds-chip-primary';
      case 'SALSA': return 'ds-chip-info';
      case 'MERENGUE': return 'ds-chip-success';
      case 'KIZOMBA': return 'ds-chip-primary';
      case 'ZOUK': return 'ds-chip-info';
      case 'AFRO': return 'ds-chip-success';
      case 'OTHER': return 'ds-chip-default';
      default: return 'ds-chip-default';
    }
  }
}
