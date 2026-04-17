import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CourseOverviewComponent } from './course-overview';
import { ActivatedRoute } from '@angular/router';
import { CourseDetail } from '../course.service';

function makeCourseDetail(overrides: Partial<CourseDetail> = {}): CourseDetail {
  return {
    id: 1,
    title: 'Bachata Fundamentals',
    danceStyle: 'BACHATA',
    level: 'BEGINNER',
    courseType: 'PARTNER',
    description: null,
    startDate: '2026-04-11',
    dayOfWeek: 'FRIDAY',
    recurrenceType: 'WEEKLY',
    numberOfSessions: 8,
    endDate: '2026-05-30',
    startTime: '19:30',
    endTime: '20:45',
    location: 'Studio A',
    teachers: null,
    maxParticipants: 20,
    roleBalancingEnabled: true,
    roleBalanceThreshold: 2,
    priceModel: 'FIXED_COURSE',
    price: 166.5,
    status: 'OPEN',
    publishedAt: null,
    enrolledStudents: 12,
    completedSessions: 0,
    ...overrides,
  };
}

describe('CourseOverviewComponent', () => {
  let fixture: ComponentFixture<CourseOverviewComponent>;
  let httpTesting: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CourseOverviewComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' }, queryParamMap: { get: () => null } } },
        },
      ],
    });

    fixture = TestBed.createComponent(CourseOverviewComponent);
    httpTesting = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement;
  });

  afterEach(() => {
    httpTesting.verify();
  });

  /** Flush the course detail request and the enrollment request that follows for non-DRAFT courses. */
  function flushCourse(overrides: Partial<CourseDetail> = {}): void {
    const course = makeCourseDetail(overrides);
    httpTesting.expectOne(req => req.url.includes('/api/courses/1') && !req.url.includes('enrollments')).flush(course);
    if (course.status !== 'DRAFT') {
      httpTesting.expectOne(req => req.url.includes('/api/courses/1/enrollments')).flush([]);
    }
  }

  it('should display loading state initially', () => {
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();

    // Flush the pending requests
    flushCourse();
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeFalsy();
  });

  it('should display course title and status chip after loading', () => {
    fixture.detectChanges();
    flushCourse({ title: 'Salsa Nights', status: 'OPEN' });
    fixture.detectChanges();

    expect(el.querySelector('.overview-title')?.textContent?.trim()).toBe('Salsa Nights');
    expect(el.querySelector('.ds-chip')?.textContent?.trim()).toBe('Open');
  });

  it('should display the back link', () => {
    fixture.detectChanges();
    flushCourse();
    fixture.detectChanges();

    const backLink = el.querySelector('.back-link') as HTMLAnchorElement;
    expect(backLink).toBeTruthy();
    expect(backLink.textContent?.trim()).toContain('Back to Courses');
  });

  it('should render the course summary component', () => {
    fixture.detectChanges();
    flushCourse();
    fixture.detectChanges();

    expect(el.querySelector('app-course-summary')).toBeTruthy();
  });

  it('should display "Course Summary" heading inside the content card', () => {
    fixture.detectChanges();
    flushCourse();
    fixture.detectChanges();

    expect(el.querySelector('.content-card-title')?.textContent?.trim()).toBe('Course Summary');
  });

  it('should not load enrollments for DRAFT courses', () => {
    fixture.detectChanges();
    flushCourse({ status: 'DRAFT' });
    fixture.detectChanges();

    // No enrollment request should have been made — httpTesting.verify() in afterEach confirms this
    expect(el.querySelector('.enrollment-section')).toBeFalsy();
  });
});
