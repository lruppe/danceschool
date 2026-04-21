import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { vi } from 'vitest';
import { StudentDetailComponent } from './student-detail';
import { StudentDetail } from '../student.service';

function makeStudent(overrides: Partial<StudentDetail> = {}): StudentDetail {
  return {
    id: 1,
    name: 'Alice Anderson',
    email: 'alice@example.com',
    phoneNumber: '+41 79 000 00 00',
    danceLevels: [
      { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
      { danceStyle: 'BACHATA', level: 'BEGINNER' },
    ],
    ...overrides,
  };
}

describe('StudentDetailComponent', () => {
  let fixture: ComponentFixture<StudentDetailComponent>;
  let httpTesting: HttpTestingController;
  let el: HTMLElement;

  function setup(studentId = '1'): void {
    TestBed.configureTestingModule({
      imports: [StudentDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => studentId } } },
        },
      ],
    });

    fixture = TestBed.createComponent(StudentDetailComponent);
    httpTesting = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement;
  }

  afterEach(() => {
    httpTesting?.verify();
  });

  function flushStudent(student: StudentDetail = makeStudent()): void {
    fixture.detectChanges();
    httpTesting.expectOne(req => req.url.endsWith(`/api/students/${student.id}`)).flush(student);
    fixture.detectChanges();
  }

  it('shows loading state before the student request resolves', () => {
    setup();
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();
    httpTesting.expectOne(req => req.url.endsWith('/api/students/1')).flush(makeStudent());
  });

  it('renders name as H1 and email + phone', () => {
    setup();
    flushStudent(makeStudent({
      name: 'Alice Anderson',
      email: 'alice@example.com',
      phoneNumber: '+41 79 000 00 00',
    }));

    expect(el.querySelector('h1.detail-title')?.textContent?.trim()).toBe('Alice Anderson');
    const dds = Array.from(el.querySelectorAll('.contact-item dd')).map(n => n.textContent?.trim());
    expect(dds).toEqual(['alice@example.com', '+41 79 000 00 00']);
  });

  it('renders em-dash for missing phone number', () => {
    setup();
    flushStudent(makeStudent({ phoneNumber: null }));

    const dds = Array.from(el.querySelectorAll('.contact-item dd')).map(n => n.textContent?.trim());
    expect(dds[1]).toBe('—');
  });

  it('renders a row per dance-level assignment with style + level chip', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
      ],
    }));

    const rows = Array.from(el.querySelectorAll('.dance-level-row'));
    expect(rows.length).toBe(2);

    const styles = rows.map(r => r.querySelector('.dance-level-style')?.textContent?.trim());
    expect(styles).toEqual(['Salsa', 'Bachata']);

    const chips = rows.map(r => r.querySelector('.ds-chip')?.textContent?.trim());
    expect(chips).toEqual(['Intermediate', 'Beginner']);
  });

  it('renders the empty state when the student has no dance levels', () => {
    setup();
    flushStudent(makeStudent({ danceLevels: [] }));

    expect(el.querySelector('.dance-level-list')).toBeNull();
    expect(el.querySelector('.dance-level-empty')?.textContent?.trim())
      .toBe('No dance levels recorded for this student yet.');
  });

  it('renders a back link to the students list', () => {
    setup();
    flushStudent();

    const back = el.querySelector('a.back-link');
    expect(back?.getAttribute('href')).toBe('/app/students');
    expect(back?.textContent?.trim()).toBe('← Back to Students');
  });

  it('shows a snackbar and redirects to the list when the student is not accessible (404)', () => {
    setup('99');
    const snackBar = TestBed.inject(MatSnackBar);
    const router = TestBed.inject(Router);
    const snackOpen = vi.spyOn(snackBar, 'open');
    const routerNavigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
    httpTesting.expectOne(req => req.url.endsWith('/api/students/99'))
      .flush({ detail: 'Student not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(snackOpen).toHaveBeenCalledTimes(1);
    expect(routerNavigate).toHaveBeenCalledWith(['/app/students']);
  });
});
