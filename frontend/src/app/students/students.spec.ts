import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { StudentsComponent } from './students';
import { StudentListItem } from './student.service';
import { AuthService } from '../shared/auth/auth.service';

function makeStudent(overrides: Partial<StudentListItem> = {}): StudentListItem {
  return {
    id: 1,
    name: 'Alice Anderson',
    email: 'alice@example.com',
    phoneNumber: '+41 79 000 00 00',
    activeCoursesCount: 1,
    ...overrides,
  };
}

function fakeAuthService(hasSchool: boolean) {
  return {
    user: signal(hasSchool
      ? {
          id: 1,
          email: 'owner@test.com',
          name: 'Owner',
          avatarUrl: null,
          memberships: [{ schoolId: 1, schoolName: 'Test', role: 'OWNER' as const }],
        }
      : {
          id: 1,
          email: 'onboarding@test.com',
          name: 'Onboarding',
          avatarUrl: null,
          memberships: [],
        }),
  };
}

describe('StudentsComponent', () => {
  let fixture: ComponentFixture<StudentsComponent>;
  let httpTesting: HttpTestingController;
  let el: HTMLElement;

  function setup(hasSchool = true): void {
    TestBed.configureTestingModule({
      imports: [StudentsComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: fakeAuthService(hasSchool) },
      ],
    });

    fixture = TestBed.createComponent(StudentsComponent);
    httpTesting = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement;
  }

  afterEach(() => {
    httpTesting?.verify();
  });

  function flushInit(students: StudentListItem[] = []): void {
    fixture.detectChanges();
    const reqs = httpTesting.match(req => req.url.includes('/api/students'));
    expect(reqs.length).toBe(1);
    reqs[0].flush(students);
    fixture.detectChanges();
  }

  it('fires exactly one GET /api/students on init', () => {
    setup();
    fixture.detectChanges();
    const reqs = httpTesting.match(req => req.url.includes('/api/students'));
    expect(reqs.length).toBe(1);
    reqs[0].flush([]);
  });

  it('skips the fetch and shows the set-up-school empty state when the user has no school', () => {
    setup(false);
    fixture.detectChanges();

    const reqs = httpTesting.match(req => req.url.includes('/api/students'));
    expect(reqs.length).toBe(0);
    expect(el.querySelector('.empty-state')).toBeTruthy();
    expect(el.querySelector('.empty-state-title')?.textContent?.trim())
      .toBe('Set up your school to get started');
  });

  it('shows loading state initially', () => {
    setup();
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();
    flushInit();
  });

  it('renders all four columns with mock data', () => {
    setup();
    flushInit([makeStudent()]);

    const headers = Array.from(el.querySelectorAll('th')).map(th => th.textContent?.trim());
    expect(headers).toEqual(['Name', 'Email', 'Phone', 'Active Courses']);

    const cells = Array.from(el.querySelectorAll('td')).map(td => td.textContent?.trim());
    expect(cells).toEqual(['Alice Anderson', 'alice@example.com', '+41 79 000 00 00', '1']);
  });

  it('renders em-dash placeholder when phoneNumber is null', () => {
    setup();
    flushInit([makeStudent({ phoneNumber: null })]);

    const phoneCell = el.querySelector('td.mat-column-phone');
    expect(phoneCell?.textContent?.trim()).toBe('—');
  });

  it('renders 2 tabs (Active, Inactive) with counts', () => {
    setup();
    flushInit([
      makeStudent({ id: 1, activeCoursesCount: 2 }),
      makeStudent({ id: 2, activeCoursesCount: 1 }),
      makeStudent({ id: 3, activeCoursesCount: 0 }),
    ]);

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    expect(tabs.length).toBe(2);

    const labels = Array.from(tabs).map(t => t.querySelector('.ds-tab-label')?.textContent?.trim());
    const counts = Array.from(tabs).map(t => t.querySelector('.ds-tab-count')?.textContent?.trim());
    expect(labels).toEqual(['Active', 'Inactive']);
    expect(counts).toEqual(['2', '1']);
  });

  it('partitions rows into Active / Inactive tabs based on activeCoursesCount > 0', () => {
    setup();
    flushInit([
      makeStudent({ id: 1, name: 'Alice',   activeCoursesCount: 2 }),
      makeStudent({ id: 2, name: 'Bob',     activeCoursesCount: 0 }),
      makeStudent({ id: 3, name: 'Carol',   activeCoursesCount: 1 }),
      makeStudent({ id: 4, name: 'Dave',    activeCoursesCount: 0 }),
    ]);

    const component = fixture.componentInstance as unknown as {
      tabData: { data: StudentListItem[] }[];
      selectTab: (i: number) => void;
    };

    const activeIds = component.tabData[0].data.map(s => s.id).sort();
    const inactiveIds = component.tabData[1].data.map(s => s.id).sort();
    expect(activeIds).toEqual([1, 3]);
    expect(inactiveIds).toEqual([2, 4]);
  });

  it('applies client-side filter on name', () => {
    setup();
    flushInit([
      makeStudent({ id: 1, name: 'Alice Anderson', email: 'alice@example.com' }),
      makeStudent({ id: 2, name: 'Bob Brown',      email: 'bob@example.com' }),
    ]);

    const component = fixture.componentInstance as unknown as {
      tabData: { filteredData: StudentListItem[] }[];
      searchText: string;
      applyFilter: () => void;
    };
    component.searchText = 'alice';
    component.applyFilter();

    expect(component.tabData[0].filteredData.map(s => s.id)).toEqual([1]);
  });

  it('applies client-side filter on email', () => {
    setup();
    flushInit([
      makeStudent({ id: 1, name: 'Alice', email: 'alice@example.com' }),
      makeStudent({ id: 2, name: 'Bob',   email: 'bob@somewhere.com' }),
    ]);

    const component = fixture.componentInstance as unknown as {
      tabData: { filteredData: StudentListItem[] }[];
      searchText: string;
      applyFilter: () => void;
    };
    component.searchText = 'somewhere';
    component.applyFilter();

    expect(component.tabData[0].filteredData.map(s => s.id)).toEqual([2]);
  });

  it('shows the current count in the footer', () => {
    setup();
    flushInit([
      makeStudent({ id: 1, name: 'Alice', activeCoursesCount: 1 }),
      makeStudent({ id: 2, name: 'Bob',   activeCoursesCount: 1 }),
    ]);

    const footer = el.querySelector('.ds-table-footer')?.textContent?.trim();
    expect(footer).toBe('Showing 2 students');
  });

  it('renders empty tab body (headers still visible) when the tab has no students', () => {
    setup();
    flushInit([makeStudent({ id: 1, activeCoursesCount: 2 })]);

    // Active tab has 1 row; Inactive tab (index 1) is empty.
    const component = fixture.componentInstance as unknown as { selectTab: (i: number) => void };
    component.selectTab(1);
    fixture.detectChanges();

    const headers = Array.from(el.querySelectorAll('th'));
    expect(headers.length).toBe(4);
    const bodyRows = Array.from(el.querySelectorAll('tr.mat-mdc-row'));
    expect(bodyRows.length).toBe(0);
  });

  it('displays error state', () => {
    setup();
    fixture.detectChanges();
    const reqs = httpTesting.match(req => req.url.includes('/api/students'));
    reqs[0].error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(el.querySelector('.error-text')).toBeTruthy();
  });
});
