import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CoursesComponent } from './courses';
import { CourseListItem } from './course.service';

function makeCourse(overrides: Partial<CourseListItem> = {}): CourseListItem {
  return {
    id: 1,
    title: 'Bachata Fundamentals',
    danceStyle: 'BACHATA',
    level: 'BEGINNER',
    dayOfWeek: 'FRIDAY',
    startTime: '19:30:00',
    endTime: '20:45:00',
    numberOfSessions: 8,
    startDate: '2026-05-15',
    endDate: '2026-07-03',
    enrolledStudents: 12,
    maxParticipants: 20,
    price: 166.5,
    status: 'RUNNING',
    completedSessions: 3,
    ...overrides,
  };
}

describe('CoursesComponent', () => {
  let fixture: ComponentFixture<CoursesComponent>;
  let httpTesting: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CoursesComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(CoursesComponent);
    httpTesting = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement;
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushAllTabs(data: {
    running?: CourseListItem[];
    open?: CourseListItem[];
    draft?: CourseListItem[];
    finished?: CourseListItem[];
  } = {}): void {
    fixture.detectChanges();
    const reqs = httpTesting.match(req => req.url.includes('/api/courses/me'));
    reqs.forEach(req => {
      const status = req.request.params.get('status');
      switch (status) {
        case 'RUNNING': req.flush(data.running ?? []); break;
        case 'OPEN': req.flush(data.open ?? []); break;
        case 'DRAFT': req.flush(data.draft ?? []); break;
        case 'FINISHED': req.flush(data.finished ?? []); break;
        default: req.flush([]);
      }
    });
    fixture.detectChanges();
  }

  it('should display loading state initially', () => {
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();

    flushAllTabs();
  });

  it('should display empty state when no courses in any tab', () => {
    flushAllTabs();
    expect(el.querySelector('.empty-state')).toBeTruthy();
    expect(el.querySelector('.empty-state-title')?.textContent?.trim()).toBe('No courses yet');
  });

  it('should render 5 tabs with labels and counts (All + 4 statuses)', () => {
    flushAllTabs({
      running: [makeCourse({ id: 1 }), makeCourse({ id: 2 })],
      open: [makeCourse({ id: 3, status: 'OPEN' })],
      draft: [],
      finished: [makeCourse({ id: 4, status: 'FINISHED' })],
    });

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    expect(tabs.length).toBe(5);

    const labels = Array.from(tabs).map(t => t.querySelector('.ds-tab-label')?.textContent?.trim());
    const counts = Array.from(tabs).map(t => t.querySelector('.ds-tab-count')?.textContent?.trim());
    expect(labels).toEqual(['All', 'Draft', 'Open', 'Running', 'Finished']);
    expect(counts).toEqual(['4', '0', '1', '2', '1']);
  });

  it('should show unified column set across tabs', () => {
    flushAllTabs({ running: [makeCourse()] });

    const headers = Array.from(el.querySelectorAll('th')).map(th => th.textContent?.trim());
    expect(headers).toEqual(['Status', 'Course Name', 'Type', 'Level', 'Start / End', 'Enrollment']);
  });

  it('should display enrollment for non-draft courses', () => {
    flushAllTabs({ running: [makeCourse({ enrolledStudents: 12, maxParticipants: 20 })] });

    const cells = Array.from(el.querySelectorAll('td')).map(td => td.textContent?.trim());
    expect(cells.some(c => c?.includes('12 / 20'))).toBe(true);
  });

  it('should leave enrollment blank for draft courses', () => {
    flushAllTabs({ draft: [makeCourse({ status: 'DRAFT', enrolledStudents: 0, maxParticipants: 20 })] });
    // Switch to Draft tab (index 1)
    const component = fixture.componentInstance as any;
    component.selectTab(1);
    fixture.detectChanges();

    const enrollmentCells = Array.from(el.querySelectorAll('td.mat-column-enrollment'));
    expect(enrollmentCells.length).toBe(1);
    expect(enrollmentCells[0].textContent?.trim()).toBe('');
  });

  it('should display status chip with dot indicator', () => {
    flushAllTabs({ running: [makeCourse()] });

    const chip = el.querySelector('.ds-chip');
    expect(chip?.querySelector('.ds-chip__dot')).toBeTruthy();
    expect(chip?.textContent?.trim()).toContain('Running');
  });

  it('should show correct count in table footer', () => {
    flushAllTabs({ running: [makeCourse({ id: 1 }), makeCourse({ id: 2 })] });
    const footer = el.querySelector('.ds-table-footer')?.textContent?.trim();
    // All tab is default and aggregates all statuses
    expect(footer).toContain('2 of 2');
  });

  it('should aggregate all statuses into the All tab', () => {
    flushAllTabs({
      running: [makeCourse({ id: 1 }), makeCourse({ id: 2 })],
      open: [makeCourse({ id: 3, status: 'OPEN' })],
      draft: [makeCourse({ id: 4, status: 'DRAFT' })],
      finished: [makeCourse({ id: 5, status: 'FINISHED' }), makeCourse({ id: 6, status: 'FINISHED' })],
    });

    const component = fixture.componentInstance as any;
    expect(component.tabCounts()[0]).toBe(6);
    expect(component.tabData[0].data.map((c: CourseListItem) => c.id).sort()).toEqual([1, 2, 3, 4, 5, 6]);
  });

  describe('search filter', () => {
    it('matches title case-insensitively', () => {
      flushAllTabs({
        running: [
          makeCourse({ id: 1, title: 'Bachata Fundamentals', danceStyle: 'BACHATA' }),
          makeCourse({ id: 2, title: 'Salsa Intermediate', danceStyle: 'SALSA' }),
        ],
      });
      const component = fixture.componentInstance as any;
      component.searchText = 'fundamentals';
      component.applyFilter();

      expect(component.tabData[0].filteredData.map((c: CourseListItem) => c.id)).toEqual([1]);
    });

    it('matches dance style, level, and day of week', () => {
      flushAllTabs({
        running: [
          makeCourse({ id: 1, danceStyle: 'BACHATA', level: 'BEGINNER', dayOfWeek: 'FRIDAY' }),
          makeCourse({ id: 2, danceStyle: 'SALSA', level: 'ADVANCED', dayOfWeek: 'MONDAY' }),
        ],
      });
      const component = fixture.componentInstance as any;

      component.searchText = 'salsa';
      component.applyFilter();
      expect(component.tabData[0].filteredData.map((c: CourseListItem) => c.id)).toEqual([2]);

      component.searchText = 'beginner';
      component.applyFilter();
      expect(component.tabData[0].filteredData.map((c: CourseListItem) => c.id)).toEqual([1]);

      component.searchText = 'monday';
      component.applyFilter();
      expect(component.tabData[0].filteredData.map((c: CourseListItem) => c.id)).toEqual([2]);
    });
  });

  it('should display error state', () => {
    fixture.detectChanges();
    const reqs = httpTesting.match(req => req.url.includes('/api/courses/me'));
    if (reqs.length > 0) {
      reqs[0].error(new ProgressEvent('error'));
    }
    fixture.detectChanges();

    expect(el.querySelector('.error-text')).toBeTruthy();
  });

  describe('helper methods', () => {
    it('should return correct status chip class', () => {
      const component = fixture.componentInstance as any;
      expect(component.statusChipClass('OPEN')).toBe('ds-chip-success');
      expect(component.statusChipClass('RUNNING')).toBe('ds-chip-primary');
      expect(component.statusChipClass('DRAFT')).toBe('ds-chip-default');
      expect(component.statusChipClass('FINISHED')).toBe('ds-chip-default');
    });

    it('should calculate session duration in minutes', () => {
      const component = fixture.componentInstance as any;
      expect(component.sessionDuration('19:30:00', '20:45:00')).toBe(75);
      expect(component.sessionDuration('18:00:00', '19:00:00')).toBe(60);
    });

    it('should format date range with short month and year on end', () => {
      const component = fixture.componentInstance as any;
      const result: string = component.formatDateRange('2026-05-15', '2026-07-03');
      expect(result).toContain('May');
      expect(result).toContain('Jul');
      expect(result).toContain('2026');
      expect(result).toContain('–');
    });

    it('should return correct dance style chip class', () => {
      const component = fixture.componentInstance as any;
      expect(component.danceStyleChipClass('BACHATA')).toBe('ds-chip-primary');
      expect(component.danceStyleChipClass('SALSA')).toBe('ds-chip-info');
      expect(component.danceStyleChipClass('MERENGUE')).toBe('ds-chip-success');
      expect(component.danceStyleChipClass('OTHER')).toBe('ds-chip-default');
    });
  });
});
