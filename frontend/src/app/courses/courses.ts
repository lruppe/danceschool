import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit, ViewChild, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { CourseListItem, CourseService } from './course.service';
import { statusChipClass } from './shared/format-utils';

interface TabConfig {
  status: string;
  label: string;
}

const COLUMNS = ['status', 'title', 'danceStyle', 'level', 'dateRange', 'enrollment'];

const TAB_CONFIGS: TabConfig[] = [
  { status: 'ACTIVE', label: 'Active' },
  { status: 'DRAFT', label: 'Draft' },
  { status: 'OPEN', label: 'Open' },
  { status: 'RUNNING', label: 'Running' },
  { status: 'FINISHED', label: 'Finished' },
];

const ACTIVE_TAB_INDEX = 0;
const DRAFT_TAB_INDEX = 1;
const OPEN_TAB_INDEX = 2;
const RUNNING_TAB_INDEX = 3;
const FINISHED_TAB_INDEX = 4;

const DEFAULT_TAB_INDEX = ACTIVE_TAB_INDEX;

const DAY_ORDER: Record<string, number> = {
  MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3,
  THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 7,
};

@Component({
  selector: 'app-courses',
  imports: [
    MatTableModule, MatSortModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatTabsModule,
    FormsModule, RouterLink, TitleCasePipe, NgClass,
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
  protected finishedError = signal(false);
  protected hasAnyCourses = signal(false);

  // Per-tab data
  protected tabData: MatTableDataSource<CourseListItem>[] = TAB_CONFIGS.map(() => new MatTableDataSource<CourseListItem>([]));
  protected tabCounts = signal<number[]>(TAB_CONFIGS.map(() => 0));

  protected searchText = '';

  protected columns = COLUMNS;
  protected activeDataSource = computed(() => this.tabData[this.activeTabIndex()]);
  protected activeTab = computed(() => TAB_CONFIGS[this.activeTabIndex()]);

  private finishedLoaded = false;

  @ViewChild(MatSort) set sort(sort: MatSort) {
    if (sort) {
      this.tabData[this.activeTabIndex()].sort = sort;
    }
  }

  ngOnInit(): void {
    this.tabData.forEach(ds => {
      ds.filterPredicate = this.createFilterPredicate();
      ds.sortingDataAccessor = (course, column) => {
        if (column === 'dateRange') return course.startDate;
        if (column === 'enrollment') return course.enrolledStudents;
        return (course as unknown as Record<string, string | number>)[column];
      };
    });

    this.courseService.getCourses().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (courses) => {
        this.partitionActiveCourses(courses);
        this.hasAnyCourses.set(courses.length > 0);
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
    if (index === FINISHED_TAB_INDEX && !this.finishedLoaded) {
      this.finishedError.set(false);
      this.loadFinishedCourses();
    }
  }

  private partitionActiveCourses(courses: CourseListItem[]): void {
    const draft: CourseListItem[] = [];
    const open: CourseListItem[] = [];
    const running: CourseListItem[] = [];
    for (const c of courses) {
      if (c.status === 'DRAFT') draft.push(c);
      else if (c.status === 'OPEN') open.push(c);
      else if (c.status === 'RUNNING') running.push(c);
    }
    this.tabData[ACTIVE_TAB_INDEX].data = courses;
    this.tabData[DRAFT_TAB_INDEX].data = draft;
    this.tabData[OPEN_TAB_INDEX].data = open;
    this.tabData[RUNNING_TAB_INDEX].data = running;

    const counts = this.tabCounts().slice();
    counts[ACTIVE_TAB_INDEX] = courses.length;
    counts[DRAFT_TAB_INDEX] = draft.length;
    counts[OPEN_TAB_INDEX] = open.length;
    counts[RUNNING_TAB_INDEX] = running.length;
    this.tabCounts.set(counts);
  }

  private loadFinishedCourses(): void {
    this.finishedLoaded = true;
    this.courseService.getCoursesByStatus('FINISHED').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (courses) => {
        this.tabData[FINISHED_TAB_INDEX].data = courses;
        const counts = this.tabCounts().slice();
        counts[FINISHED_TAB_INDEX] = courses.length;
        this.tabCounts.set(counts);
        if (courses.length > 0) this.hasAnyCourses.set(true);
      },
      error: () => {
        // Allow retry on next tab activation
        this.finishedLoaded = false;
        this.finishedError.set(true);
      },
    });
  }

  protected applyFilter(): void {
    const text = this.searchText.trim().toLowerCase();
    this.tabData.forEach(ds => ds.filter = text);
  }

  private createFilterPredicate(): (data: CourseListItem, filter: string) => boolean {
    return (data: CourseListItem, filter: string): boolean => {
      if (!filter) return true;
      const searchable = [data.title, data.danceStyle, data.level, data.dayOfWeek].join(' ').toLowerCase();
      return searchable.includes(filter);
    };
  }

  protected formatDateRange(startDate: string, endDate: string): string {
    const opts: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
    const start = new Date(startDate + 'T00:00:00');
    const end = new Date(endDate + 'T00:00:00');
    const startStr = start.toLocaleDateString(undefined, opts);
    const endStr = end.toLocaleDateString(undefined, { ...opts, year: 'numeric' });
    return `${startStr} – ${endStr}`;
  }

  protected sessionDuration(startTime: string, endTime: string): number {
    const [startH, startM] = startTime.split(':').map(Number);
    const [endH, endM] = endTime.split(':').map(Number);
    return (endH * 60 + endM) - (startH * 60 + startM);
  }

  protected statusChipClass = statusChipClass;

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
