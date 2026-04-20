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
    courseType: 'PARTNER',
    dayOfWeek: 'FRIDAY',
    startTime: '19:30:00',
    endTime: '20:45:00',
    numberOfSessions: 8,
    startDate: '2026-05-15',
    endDate: '2026-07-03',
    enrolledStudents: 12,
    leadCount: 5,
    followCount: 7,
    maxParticipants: 20,
    price: 166.5,
    status: 'RUNNING',
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

  /**
   * Flushes the single init request (GET /api/courses/me without status) with `courses`.
   * The backend excludes FINISHED from this response; the component lazy-loads FINISHED
   * on first activation.
   */
  function flushInit(courses: CourseListItem[] = []): void {
    fixture.detectChanges();
    const reqs = httpTesting.match(req =>
      req.url.includes('/api/courses/me') && !req.params.has('status'));
    expect(reqs.length).toBe(1);
    reqs[0].flush(courses);
    fixture.detectChanges();
  }

  it('fires exactly one GET /api/courses/me (no status) on init', () => {
    fixture.detectChanges();
    const noStatus = httpTesting.match(req =>
      req.url.includes('/api/courses/me') && !req.params.has('status'));
    const withStatus = httpTesting.match(req =>
      req.url.includes('/api/courses/me') && req.params.has('status'));
    expect(noStatus.length).toBe(1);
    expect(withStatus.length).toBe(0);
    noStatus[0].flush([]);
  });

  it('surfaces an error and allows retry when FINISHED fetch fails', () => {
    flushInit([makeCourse({ id: 1, status: 'RUNNING' })]);

    const component = fixture.componentInstance as any;
    component.selectTab(4);
    fixture.detectChanges();

    const failed = httpTesting.match(req =>
      req.url.includes('/api/courses/me') && req.params.get('status') === 'FINISHED');
    expect(failed.length).toBe(1);
    failed[0].error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(el.querySelector('.error-text')).toBeTruthy();
    expect(component.finishedError()).toBe(true);

    // Re-activating the tab retries the fetch
    component.selectTab(4);
    fixture.detectChanges();
    const retried = httpTesting.match(req =>
      req.url.includes('/api/courses/me') && req.params.get('status') === 'FINISHED');
    expect(retried.length).toBe(1);
    retried[0].flush([]);
  });

  it('loads FINISHED lazily on first Finished tab activation and caches it', () => {
    flushInit([makeCourse({ id: 1, status: 'RUNNING' })]);

    // No FINISHED request before the tab is activated
    expect(httpTesting.match(req =>
      req.url.includes('/api/courses/me') && req.params.get('status') === 'FINISHED').length).toBe(0);

    const component = fixture.componentInstance as any;
    component.selectTab(4);
    fixture.detectChanges();

    const finishedReqs = httpTesting.match(req =>
      req.url.includes('/api/courses/me') && req.params.get('status') === 'FINISHED');
    expect(finishedReqs.length).toBe(1);
    finishedReqs[0].flush([makeCourse({ id: 9, status: 'FINISHED' })]);
    fixture.detectChanges();

    // Switch away and back: no refetch
    component.selectTab(0);
    fixture.detectChanges();
    component.selectTab(4);
    fixture.detectChanges();
    expect(httpTesting.match(req =>
      req.url.includes('/api/courses/me') && req.params.get('status') === 'FINISHED').length).toBe(0);
  });

  it('should display loading state initially', () => {
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();

    flushInit();
  });

  it('should display empty state when no active courses', () => {
    flushInit([]);
    expect(el.querySelector('.empty-state')).toBeTruthy();
    expect(el.querySelector('.empty-state-title')?.textContent?.trim()).toBe('No courses yet');
  });

  it('should render 5 tabs (Active + 4 statuses) with labels and counts', () => {
    flushInit([
      makeCourse({ id: 1, status: 'RUNNING' }),
      makeCourse({ id: 2, status: 'RUNNING' }),
      makeCourse({ id: 3, status: 'OPEN' }),
      makeCourse({ id: 4, status: 'DRAFT' }),
    ]);

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    expect(tabs.length).toBe(5);

    const labels = Array.from(tabs).map(t => t.querySelector('.ds-tab-label')?.textContent?.trim());
    const counts = Array.from(tabs).map(t => t.querySelector('.ds-tab-count')?.textContent?.trim());
    expect(labels).toEqual(['Active', 'Draft', 'Open', 'Running', 'Finished']);
    // Active = DRAFT + OPEN + RUNNING; Finished is 0 until lazy-loaded
    expect(counts).toEqual(['4', '1', '1', '2', '0']);
  });

  it('shows unified column set across tabs', () => {
    flushInit([makeCourse()]);

    const headers = Array.from(el.querySelectorAll('th')).map(th => th.textContent?.trim());
    expect(headers).toEqual(['Status', 'Course Name', 'Type', 'Level', 'Start / End', 'Enrollment']);
  });

  it('displays enrollment for non-draft courses', () => {
    flushInit([makeCourse({ enrolledStudents: 12, maxParticipants: 20 })]);

    const cells = Array.from(el.querySelectorAll('td')).map(td => td.textContent?.trim());
    expect(cells.some(c => c?.includes('12 / 20'))).toBe(true);
  });

  it('shows leads/follows sub-line for PARTNER courses', () => {
    flushInit([makeCourse({ courseType: 'PARTNER', leadCount: 5, followCount: 7 })]);

    const cell = el.querySelector('td.mat-column-enrollment');
    expect(cell?.querySelector('.ds-cell-primary')?.textContent?.trim()).toBe('12 / 20');
    expect(cell?.querySelector('.ds-cell-secondary')?.textContent?.trim()).toBe('5L / 7F');
  });

  it('does NOT show leads/follows sub-line for SOLO courses', () => {
    flushInit([makeCourse({ courseType: 'SOLO', leadCount: 0, followCount: 0 })]);

    const cell = el.querySelector('td.mat-column-enrollment');
    expect(cell?.querySelector('.ds-cell-primary')?.textContent?.trim()).toBe('12 / 20');
    expect(cell?.querySelector('.ds-cell-secondary')).toBeNull();
  });

  it('leaves enrollment blank for draft courses', () => {
    flushInit([makeCourse({ status: 'DRAFT', enrolledStudents: 0, maxParticipants: 20 })]);
    // Switch to Draft tab (index 1)
    const component = fixture.componentInstance as any;
    component.selectTab(1);
    fixture.detectChanges();

    const enrollmentCells = Array.from(el.querySelectorAll('td.mat-column-enrollment'));
    expect(enrollmentCells.length).toBe(1);
    expect(enrollmentCells[0].textContent?.trim()).toBe('');
  });

  it('displays status chip with dot indicator', () => {
    flushInit([makeCourse()]);

    const chip = el.querySelector('.ds-chip');
    expect(chip?.querySelector('.ds-chip__dot')).toBeTruthy();
    expect(chip?.textContent?.trim()).toContain('Running');
  });

  it('shows correct count in table footer', () => {
    flushInit([makeCourse({ id: 1 }), makeCourse({ id: 2 })]);
    const footer = el.querySelector('.ds-table-footer')?.textContent?.trim();
    // Active tab is default and contains all non-FINISHED courses from the init response
    expect(footer).toContain('2 of 2');
  });

  it('aggregates non-FINISHED statuses into the Active tab', () => {
    flushInit([
      makeCourse({ id: 1, status: 'RUNNING' }),
      makeCourse({ id: 2, status: 'RUNNING' }),
      makeCourse({ id: 3, status: 'OPEN' }),
      makeCourse({ id: 4, status: 'DRAFT' }),
    ]);

    const component = fixture.componentInstance as any;
    expect(component.tabCounts()[0]).toBe(4);
    expect(component.tabData[0].data.map((c: CourseListItem) => c.id).sort()).toEqual([1, 2, 3, 4]);
  });

  describe('search filter', () => {
    it('matches title case-insensitively', () => {
      flushInit([
        makeCourse({ id: 1, title: 'Bachata Fundamentals', danceStyle: 'BACHATA' }),
        makeCourse({ id: 2, title: 'Salsa Intermediate', danceStyle: 'SALSA' }),
      ]);
      const component = fixture.componentInstance as any;
      component.searchText = 'fundamentals';
      component.applyFilter();

      expect(component.tabData[0].filteredData.map((c: CourseListItem) => c.id)).toEqual([1]);
    });

    it('matches dance style, level, and day of week', () => {
      flushInit([
        makeCourse({ id: 1, danceStyle: 'BACHATA', level: 'BEGINNER', dayOfWeek: 'FRIDAY' }),
        makeCourse({ id: 2, danceStyle: 'SALSA', level: 'ADVANCED', dayOfWeek: 'MONDAY' }),
      ]);
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

  it('displays error state', () => {
    fixture.detectChanges();
    const reqs = httpTesting.match(req => req.url.includes('/api/courses/me'));
    if (reqs.length > 0) {
      reqs[0].error(new ProgressEvent('error'));
    }
    fixture.detectChanges();

    expect(el.querySelector('.error-text')).toBeTruthy();
  });

  describe('helper methods', () => {
    it('returns correct status chip class', () => {
      const component = fixture.componentInstance as any;
      expect(component.statusChipClass('OPEN')).toBe('ds-chip-success');
      expect(component.statusChipClass('RUNNING')).toBe('ds-chip-primary');
      expect(component.statusChipClass('DRAFT')).toBe('ds-chip-default');
      expect(component.statusChipClass('FINISHED')).toBe('ds-chip-default');
    });

    it('calculates session duration in minutes', () => {
      const component = fixture.componentInstance as any;
      expect(component.sessionDuration('19:30:00', '20:45:00')).toBe(75);
      expect(component.sessionDuration('18:00:00', '19:00:00')).toBe(60);
    });

    it('formats date range with short month and year on end', () => {
      const component = fixture.componentInstance as any;
      const result: string = component.formatDateRange('2026-05-15', '2026-07-03');
      expect(result).toContain('May');
      expect(result).toContain('Jul');
      expect(result).toContain('2026');
      expect(result).toContain('–');
    });

    it('returns correct dance style chip class', () => {
      const component = fixture.componentInstance as any;
      expect(component.danceStyleChipClass('BACHATA')).toBe('ds-chip-primary');
      expect(component.danceStyleChipClass('SALSA')).toBe('ds-chip-info');
      expect(component.danceStyleChipClass('MERENGUE')).toBe('ds-chip-success');
      expect(component.danceStyleChipClass('OTHER')).toBe('ds-chip-default');
    });
  });
});
