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
    endDate: '2026-05-30',
    enrolledStudents: 12,
    maxParticipants: 20,
    price: 166.5,
    status: 'ACTIVE',
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

  function flushCourses(courses: CourseListItem[]): void {
    fixture.detectChanges();
    httpTesting.expectOne(req => req.url.includes('/api/courses/me')).flush(courses);
    fixture.detectChanges();
  }

  it('should display loading state initially', () => {
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();

    httpTesting.expectOne(req => req.url.includes('/api/courses/me')).flush([]);
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeFalsy();
  });

  it('should display empty state when no courses', () => {
    flushCourses([]);
    expect(el.querySelector('.empty-state')).toBeTruthy();
    expect(el.querySelector('.empty-state-title')?.textContent?.trim()).toBe('No courses yet');
  });

  it('should display all table columns without Actions column', () => {
    flushCourses([makeCourse()]);
    const headers = Array.from(el.querySelectorAll('th')).map(th => th.textContent?.trim());
    expect(headers).toContain('Course Name');
    expect(headers).toContain('Type');
    expect(headers).toContain('Level');
    expect(headers).toContain('Schedule');
    expect(headers).toContain('Enrollment');
    expect(headers).toContain('Price');
    expect(headers).toContain('Status');
    expect(headers).not.toContain('Actions');
  });

  it('should display course data in table rows', () => {
    flushCourses([makeCourse({ title: 'Salsa Nights', enrolledStudents: 5, maxParticipants: 15 })]);
    const cells = Array.from(el.querySelectorAll('td')).map(td => td.textContent?.trim());
    expect(cells).toContain('Salsa Nights');
    expect(cells.some(c => c?.includes('5 / 15'))).toBe(true);
  });

  it('should show correct count in table footer', () => {
    flushCourses([makeCourse(), makeCourse({ id: 2, title: 'Salsa' })]);
    const footer = el.querySelector('.ds-table-footer')?.textContent?.trim();
    expect(footer).toContain('2 of 2');
  });

  it('should display error state', () => {
    fixture.detectChanges();
    httpTesting.expectOne(req => req.url.includes('/api/courses/me')).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(el.querySelector('.error-text')).toBeTruthy();
  });

  describe('filter predicate', () => {
    it('should filter by free-text search across title', () => {
      flushCourses([
        makeCourse({ id: 1, title: 'Bachata Fundamentals' }),
        makeCourse({ id: 2, title: 'Salsa Nights' }),
      ]);

      const component = fixture.componentInstance as any;
      component.searchText = 'salsa';
      component.applyFilter();
      fixture.detectChanges();

      const rows = el.querySelectorAll('tr.clickable-row');
      expect(rows.length).toBe(1);
    });

    it('should filter by dance style', () => {
      flushCourses([
        makeCourse({ id: 1, danceStyle: 'BACHATA' }),
        makeCourse({ id: 2, danceStyle: 'SALSA' }),
      ]);

      const component = fixture.componentInstance as any;
      component.selectedDanceStyle = 'BACHATA';
      component.applyFilter();
      fixture.detectChanges();

      const rows = el.querySelectorAll('tr.clickable-row');
      expect(rows.length).toBe(1);
    });

    it('should filter by level', () => {
      flushCourses([
        makeCourse({ id: 1, level: 'BEGINNER' }),
        makeCourse({ id: 2, level: 'INTERMEDIATE' }),
      ]);

      const component = fixture.componentInstance as any;
      component.selectedLevel = 'BEGINNER';
      component.applyFilter();
      fixture.detectChanges();

      const rows = el.querySelectorAll('tr.clickable-row');
      expect(rows.length).toBe(1);
    });

    it('should combine filters', () => {
      flushCourses([
        makeCourse({ id: 1, title: 'Bachata Beginner', danceStyle: 'BACHATA', level: 'BEGINNER' }),
        makeCourse({ id: 2, title: 'Bachata Advanced', danceStyle: 'BACHATA', level: 'ADVANCED' }),
        makeCourse({ id: 3, title: 'Salsa Beginner', danceStyle: 'SALSA', level: 'BEGINNER' }),
      ]);

      const component = fixture.componentInstance as any;
      component.selectedDanceStyle = 'BACHATA';
      component.selectedLevel = 'BEGINNER';
      component.applyFilter();
      fixture.detectChanges();

      const rows = el.querySelectorAll('tr.clickable-row');
      expect(rows.length).toBe(1);
    });
  });

  describe('helper methods', () => {
    it('should return correct status chip class', () => {
      const component = fixture.componentInstance as any;
      expect(component.statusChipClass('ACTIVE')).toBe('ds-chip-success');
      expect(component.statusChipClass('DRAFT')).toBe('ds-chip-default');
      expect(component.statusChipClass('INACTIVE')).toBe('ds-chip-default');
    });

    it('should return correct dance style chip class', () => {
      const component = fixture.componentInstance as any;
      expect(component.danceStyleChipClass('BACHATA')).toBe('ds-chip-primary');
      expect(component.danceStyleChipClass('SALSA')).toBe('ds-chip-info');
      expect(component.danceStyleChipClass('MERENGUE')).toBe('ds-chip-success');
      expect(component.danceStyleChipClass('OTHER')).toBe('ds-chip-default');
    });

    it('should calculate session duration in minutes', () => {
      const component = fixture.componentInstance as any;
      expect(component.sessionDuration('19:30:00', '20:45:00')).toBe(75);
      expect(component.sessionDuration('18:00:00', '19:00:00')).toBe(60);
    });
  });
});
