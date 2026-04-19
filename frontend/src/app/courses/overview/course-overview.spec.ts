import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CourseOverviewComponent } from './course-overview';
import { ActivatedRoute } from '@angular/router';
import { CourseDetail } from '../course.service';
import { EnrollmentListItem } from '../enrollment.service';

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

  /** Flush the course detail request and the enrollment list with provided items. */
  function flushCourseWithEnrollments(enrollments: EnrollmentListItem[], overrides: Partial<CourseDetail> = {}): void {
    const course = makeCourseDetail(overrides);
    httpTesting.expectOne(req => req.url.includes('/api/courses/1') && !req.url.includes('enrollments')).flush(course);
    httpTesting.expectOne(req => req.url.includes('/api/courses/1/enrollments')).flush(enrollments);
  }

  function makeEnrollment(overrides: Partial<EnrollmentListItem> = {}): EnrollmentListItem {
    return {
      id: 1,
      studentName: 'Anna Mueller',
      studentEmail: 'anna@example.com',
      studentPhoneNumber: '+41 79 100 0001',
      danceRole: 'FOLLOW',
      status: 'PENDING_APPROVAL',
      enrolledAt: '2026-04-10T10:00:00Z',
      approvedAt: null,
      paidAt: null,
      waitlistPosition: null,
      waitlistReason: null,
      studentDanceLevel: 'INTERMEDIATE',
      ...overrides,
    };
  }

  describe('Approve tab', () => {
    it('renders PENDING_APPROVAL rows with level chip and action buttons', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([makeEnrollment()]);
      fixture.detectChanges();

      // Switch to the Approve tab (index 2)
      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[2] as HTMLElement).click();
      fixture.detectChanges();

      const row = el.querySelector('tr[mat-row]');
      expect(row).toBeTruthy();
      expect(row?.textContent).toContain('Anna Mueller');

      const levelChip = row?.querySelector('.approve-cell .ds-chip');
      expect(levelChip?.textContent?.trim()).toBe('Intermediate');
      expect(levelChip?.classList.contains('ds-chip-primary')).toBe(true);

      expect(row?.querySelector('button[aria-label="Approve enrollment"]')).toBeTruthy();
      expect(row?.querySelector('button[aria-label="Reject enrollment"]')).toBeTruthy();
    });

    it('shows "No level" chip when studentDanceLevel is null', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([makeEnrollment({ studentDanceLevel: null })]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[2] as HTMLElement).click();
      fixture.detectChanges();

      const levelChip = el.querySelector('tr[mat-row] .approve-cell .ds-chip');
      expect(levelChip?.textContent?.trim()).toBe('No level');
    });

    it('calls approve endpoint and refreshes list when approve clicked', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([makeEnrollment({ id: 42 })]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[2] as HTMLElement).click();
      fixture.detectChanges();

      const approveBtn = el.querySelector('button[aria-label="Approve enrollment"]') as HTMLButtonElement;
      approveBtn.click();
      fixture.detectChanges();

      const approveReq = httpTesting.expectOne(req =>
        req.url.includes('/api/enrollments/42/approve') && req.method === 'PUT');
      approveReq.flush({ enrollmentId: 42, status: 'PENDING_PAYMENT' });

      // List refresh: the component re-fetches enrollments
      httpTesting.expectOne(req => req.url.includes('/api/courses/1/enrollments')).flush([]);
    });

    it('calls reject endpoint and refreshes list when reject clicked', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([makeEnrollment({ id: 77 })]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[2] as HTMLElement).click();
      fixture.detectChanges();

      const rejectBtn = el.querySelector('button[aria-label="Reject enrollment"]') as HTMLButtonElement;
      rejectBtn.click();
      fixture.detectChanges();

      const rejectReq = httpTesting.expectOne(req =>
        req.url.includes('/api/enrollments/77/reject') && req.method === 'PUT');
      rejectReq.flush({ enrollmentId: 77, status: 'REJECTED' });

      httpTesting.expectOne(req => req.url.includes('/api/courses/1/enrollments')).flush([]);
    });
  });

  describe('Open Payment tab', () => {
    it('renders Mark Paid button for approved-then-pending-payment enrollments (approvedAt set)', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([
        makeEnrollment({
          id: 50, studentName: 'Paid Me', status: 'PENDING_PAYMENT',
          approvedAt: '2026-04-15T12:00:00Z', paidAt: null,
        }),
      ]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[3] as HTMLElement).click();
      fixture.detectChanges();

      const row = el.querySelector('tr[mat-row]');
      expect(row).toBeTruthy();
      expect(row?.textContent).toContain('Paid Me');

      const markPaidBtn = row?.querySelector('.mark-paid-button') as HTMLButtonElement;
      expect(markPaidBtn).toBeTruthy();
      expect(markPaidBtn.textContent?.trim()).toBe('Mark Paid');
    });

    it('calls markPaid endpoint and refreshes list when Mark Paid clicked', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([
        makeEnrollment({ id: 99, status: 'PENDING_PAYMENT', approvedAt: '2026-04-15T12:00:00Z' }),
      ]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[3] as HTMLElement).click();
      fixture.detectChanges();

      const markPaidBtn = el.querySelector('.mark-paid-button') as HTMLButtonElement;
      markPaidBtn.click();
      fixture.detectChanges();

      const markPaidReq = httpTesting.expectOne(req =>
        req.url.includes('/api/enrollments/99/mark-paid') && req.method === 'PUT');
      markPaidReq.flush({ enrollmentId: 99, status: 'CONFIRMED' });

      httpTesting.expectOne(req => req.url.includes('/api/courses/1/enrollments')).flush([]);
    });
  });

  describe('Waitlist tab', () => {
    it('renders WAITLISTED rows with position and reason chips', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([
        makeEnrollment({
          id: 10, studentName: 'Lead A', status: 'WAITLISTED', danceRole: 'LEAD',
          waitlistPosition: 2, waitlistReason: 'ROLE_IMBALANCE',
        }),
      ]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[1] as HTMLElement).click();
      fixture.detectChanges();

      const row = el.querySelector('tr[mat-row]');
      expect(row).toBeTruthy();
      expect(row?.textContent).toContain('Lead A');

      const waitlistCell = row?.querySelector('.waitlist-cell');
      expect(waitlistCell).toBeTruthy();

      const chips = waitlistCell?.querySelectorAll('.ds-chip') ?? [];
      expect(chips.length).toBe(2);
      expect(chips[0].textContent?.trim()).toBe('#2');
      expect(chips[1].textContent?.trim()).toBe('Role imbalance');
      expect(chips[1].classList.contains('ds-chip-info')).toBe(true);
    });

    it('renders CAPACITY reason with default chip styling', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([
        makeEnrollment({
          status: 'WAITLISTED', waitlistPosition: 1, waitlistReason: 'CAPACITY',
        }),
      ]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[1] as HTMLElement).click();
      fixture.detectChanges();

      const chips = el.querySelectorAll('.waitlist-cell .ds-chip');
      expect(chips[0].textContent?.trim()).toBe('#1');
      expect(chips[1].textContent?.trim()).toBe('Capacity');
      expect(chips[1].classList.contains('ds-chip-default')).toBe(true);
    });

    it('shows Waitlist as the last-column header on the waitlist tab', () => {
      fixture.detectChanges();
      flushCourseWithEnrollments([
        makeEnrollment({
          status: 'WAITLISTED', waitlistPosition: 1, waitlistReason: 'CAPACITY',
        }),
      ]);
      fixture.detectChanges();

      const tabLinks = el.querySelectorAll('a[mat-tab-link]');
      (tabLinks[1] as HTMLElement).click();
      fixture.detectChanges();

      const headers = Array.from(el.querySelectorAll('th[mat-header-cell]')).map(h => h.textContent?.trim());
      expect(headers).toContain('Waitlist');
    });
  });
});
