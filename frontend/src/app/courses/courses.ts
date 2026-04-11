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
import { forkJoin } from 'rxjs';
import { CourseListItem, CourseService } from './course.service';
import { formatDayShort, formatTime as stripSeconds } from './shared/format-utils';
import { CourseStatus, DANCE_STYLES, COURSE_LEVELS } from '../shared/course-constants';

interface CourseFilter {
  text: string;
  danceStyle: string;
  level: string;
}

interface TabConfig {
  status: CourseStatus;
  label: string;
  columns: string[];
}

const TAB_CONFIGS: TabConfig[] = [
  { status: 'DRAFT', label: 'Draft', columns: ['status', 'title', 'danceStyle', 'level', 'schedule', 'price', 'readiness'] },
  { status: 'OPEN', label: 'Open', columns: ['status', 'title', 'danceStyle', 'level', 'schedule', 'enrollment', 'startsIn'] },
  { status: 'RUNNING', label: 'Running', columns: ['status', 'title', 'danceStyle', 'level', 'schedule', 'progress', 'participants'] },
  { status: 'FINISHED', label: 'Finished', columns: ['status', 'title', 'danceStyle', 'level', 'schedule', 'participants'] },
];

const DEFAULT_TAB_INDEX = 2; // Running tab

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

  protected tabs = TAB_CONFIGS;
  protected activeTabIndex = signal(DEFAULT_TAB_INDEX);
  protected loaded = signal(false);
  protected error = signal(false);
  protected hasAnyCourses = signal(false);

  // Per-tab data
  protected tabData: MatTableDataSource<CourseListItem>[] = TAB_CONFIGS.map(() => new MatTableDataSource<CourseListItem>([]));
  protected tabCounts = signal<number[]>([0, 0, 0, 0]);

  protected searchText = '';
  protected selectedDanceStyle = '';
  protected selectedLevel = '';

  protected danceStyles = DANCE_STYLES.map(d => d.value);
  protected levels = COURSE_LEVELS.map(l => l.value);

  protected activeDataSource = computed(() => this.tabData[this.activeTabIndex()]);
  protected activeTab = computed(() => TAB_CONFIGS[this.activeTabIndex()]);

  @ViewChild(MatSort) set sort(sort: MatSort) {
    if (sort) {
      this.tabData[this.activeTabIndex()].sort = sort;
    }
  }

  ngOnInit(): void {
    this.tabData.forEach(ds => {
      ds.filterPredicate = this.createFilterPredicate();
      ds.sortingDataAccessor = (course, column) => {
        if (column === 'schedule') return DAY_ORDER[course.dayOfWeek] ?? 0;
        return (course as unknown as Record<string, string | number>)[column];
      };
    });

    forkJoin(
      TAB_CONFIGS.map(tab => this.courseService.getCoursesByStatus(tab.status))
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (results) => {
        const counts: number[] = [];
        results.forEach((courses, i) => {
          this.tabData[i].data = courses;
          counts.push(courses.length);
        });
        this.tabCounts.set(counts);
        this.hasAnyCourses.set(counts.some(c => c > 0));
        this.loaded.set(true);
      },
      error: () => {
        this.error.set(true);
        this.loaded.set(true);
      },
    });
  }

  protected selectTab(index: number): void {
    this.activeTabIndex.set(index);
  }

  protected applyFilter(): void {
    const filter: CourseFilter = {
      text: this.searchText.trim().toLowerCase(),
      danceStyle: this.selectedDanceStyle,
      level: this.selectedLevel,
    };
    const serialized = JSON.stringify(filter);
    this.tabData.forEach(ds => ds.filter = serialized);
  }

  private createFilterPredicate(): (data: CourseListItem, filter: string) => boolean {
    return (data: CourseListItem, filter: string): boolean => {
      const f: CourseFilter = JSON.parse(filter);
      if (f.danceStyle && data.danceStyle !== f.danceStyle) return false;
      if (f.level && data.level !== f.level) return false;
      if (f.text) {
        const searchable = [data.title, data.danceStyle, data.level, data.dayOfWeek].join(' ').toLowerCase();
        if (!searchable.includes(f.text)) return false;
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

  protected startsIn(startDate: string): string {
    const start = new Date(startDate + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const days = Math.ceil((start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    if (days <= 0) return 'Today';
    if (days === 1) return '1 day';
    return `${days} days`;
  }

  protected statusChipClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'ds-chip-success';
      case 'RUNNING': return 'ds-chip-primary';
      case 'DRAFT': return 'ds-chip-default';
      case 'FINISHED': return 'ds-chip-default';
      default: return 'ds-chip-default';
    }
  }

  protected statusEmoji(status: string): string {
    switch (status) {
      case 'DRAFT': return '🟡';
      case 'OPEN': return '🟢';
      case 'RUNNING': return '🔵';
      case 'FINISHED': return '⚫';
      default: return '';
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
