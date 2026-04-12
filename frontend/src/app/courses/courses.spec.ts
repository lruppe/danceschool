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

  it('should render 4 tabs with correct labels and counts', () => {
    flushAllTabs({
      running: [makeCourse({ id: 1 }), makeCourse({ id: 2 })],
      open: [makeCourse({ id: 3, status: 'OPEN' })],
      draft: [],
      finished: [makeCourse({ id: 4, status: 'FINISHED' })],
    });

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    expect(tabs.length).toBe(4);
    // Order: Draft, Open, Running, Finished
    expect(tabs[0].textContent?.trim()).toBe('Draft (0)');
    expect(tabs[1].textContent?.trim()).toBe('Open (1)');
    expect(tabs[2].textContent?.trim()).toBe('Running (2)');
    expect(tabs[3].textContent?.trim()).toBe('Finished (1)');
  });

  it('should show Running tab columns by default', () => {
    flushAllTabs({ running: [makeCourse()] });

    const headers = Array.from(el.querySelectorAll('th')).map(th => th.textContent?.trim());
    expect(headers).toContain('Status');
    expect(headers).toContain('Course Name');
    expect(headers).toContain('Progress');
    expect(headers).toContain('Participants');
    expect(headers).not.toContain('Price');
    expect(headers).not.toContain('Starts In');
  });

  it('should display progress in Running tab', () => {
    flushAllTabs({ running: [makeCourse({ completedSessions: 3, numberOfSessions: 8 })] });

    const cells = Array.from(el.querySelectorAll('td')).map(td => td.textContent?.trim());
    expect(cells.some(c => c?.includes('Session 3/8'))).toBe(true);
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
    expect(footer).toContain('2 of 2');
  });

  it('should display error state', () => {
    fixture.detectChanges();
    // Error the first request — forkJoin cancels the rest
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

    it('should calculate starts in days', () => {
      const component = fixture.componentInstance as any;
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 10);
      const dateStr = futureDate.toISOString().split('T')[0];
      expect(component.startsIn(dateStr)).toBe('10 days');
    });

    it('should return "1 day" for tomorrow', () => {
      const component = fixture.componentInstance as any;
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const dateStr = tomorrow.toISOString().split('T')[0];
      expect(component.startsIn(dateStr)).toBe('1 day');
    });

    it('should return "Today" for today or past dates', () => {
      const component = fixture.componentInstance as any;
      const today = new Date();
      const dateStr = today.toISOString().split('T')[0];
      expect(component.startsIn(dateStr)).toBe('Today');
    });

    it('should calculate session duration in minutes', () => {
      const component = fixture.componentInstance as any;
      expect(component.sessionDuration('19:30:00', '20:45:00')).toBe(75);
      expect(component.sessionDuration('18:00:00', '19:00:00')).toBe(60);
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
