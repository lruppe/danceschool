import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { vi } from 'vitest';
import { StudentDetailComponent } from './student-detail';
import { StudentDetail, UpdateDanceLevelsResult } from '../student.service';

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

function makeUpdateResult(student: StudentDetail, autoConfirmedCount = 0): UpdateDanceLevelsResult {
  return { student, autoConfirmedCount };
}

describe('StudentDetailComponent', () => {
  let fixture: ComponentFixture<StudentDetailComponent>;
  let component: StudentDetailComponent;
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
    component = fixture.componentInstance;
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

  // Non-private component accessors for driving edits from tests.
  type PrivateComponent = {
    onStyleChange(i: number, v: string): void;
    onLevelChange(i: number, v: string): void;
    addRow(): void;
    onCancel(): void;
    onSave(): void;
    availableStylesFor(i: number): { value: string; label: string }[];
    edits(): { danceStyle: string | null; level: string | null }[];
    hasValidChanges(): boolean;
    canAddRow(): boolean;
  };
  const drive = (c: unknown): PrivateComponent => c as PrivateComponent;

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

  it('renders a row per dance-level assignment with the style label visible', () => {
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

    // Each existing row shows one (level) select; no style select.
    expect(rows[0].querySelectorAll('mat-form-field').length).toBe(1);
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

  // ── Inline editing ──

  it('Save is disabled on initial load (no-op)', () => {
    setup();
    flushStudent();

    expect(drive(component).hasValidChanges()).toBe(false);
    const saveBtn = el.querySelector<HTMLButtonElement>('.dance-level-actions button[mat-flat-button]');
    expect(saveBtn?.disabled).toBe(true);
  });

  it('Save sends the edited set unchanged-for-unedited rows (edit-only)', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
      ],
    }));

    drive(component).onLevelChange(0, 'ADVANCED');
    fixture.detectChanges();
    expect(drive(component).hasValidChanges()).toBe(true);

    drive(component).onSave();
    const req = httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels') && r.method === 'PUT');
    expect(req.request.body).toEqual({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'ADVANCED' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
      ],
    });
    req.flush(makeUpdateResult(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'ADVANCED' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
      ],
    })));
  });

  it('Save sends appended rows (add-only)', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'INTERMEDIATE' }],
    }));

    drive(component).addRow();
    drive(component).onStyleChange(1, 'KIZOMBA');
    drive(component).onLevelChange(1, 'STARTER');
    fixture.detectChanges();
    expect(drive(component).hasValidChanges()).toBe(true);

    drive(component).onSave();
    const req = httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels') && r.method === 'PUT');
    expect(req.request.body).toEqual({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'KIZOMBA', level: 'STARTER' },
      ],
    });
    req.flush(makeUpdateResult(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'KIZOMBA', level: 'STARTER' },
      ],
    })));
  });

  it('Save sends the merged set (mixed edit + add)', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
      ],
    }));

    drive(component).onLevelChange(1, 'INTERMEDIATE');
    drive(component).addRow();
    drive(component).onStyleChange(2, 'ZOUK');
    drive(component).onLevelChange(2, 'BEGINNER');
    fixture.detectChanges();
    expect(drive(component).hasValidChanges()).toBe(true);

    drive(component).onSave();
    const req = httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels') && r.method === 'PUT');
    expect(req.request.body).toEqual({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'INTERMEDIATE' },
        { danceStyle: 'ZOUK', level: 'BEGINNER' },
      ],
    });
    req.flush(makeUpdateResult(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'INTERMEDIATE' },
        { danceStyle: 'ZOUK', level: 'BEGINNER' },
      ],
    })));
  });

  it('Add-level is disabled once all 7 styles are assigned', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
        { danceStyle: 'MERENGUE', level: 'BEGINNER' },
        { danceStyle: 'KIZOMBA', level: 'BEGINNER' },
        { danceStyle: 'ZOUK', level: 'BEGINNER' },
        { danceStyle: 'AFRO', level: 'BEGINNER' },
        { danceStyle: 'OTHER', level: 'BEGINNER' },
      ],
    }));

    expect(drive(component).canAddRow()).toBe(false);
    const addBtn = el.querySelector<HTMLButtonElement>('.dance-level-add button');
    expect(addBtn?.disabled).toBe(true);
  });

  it('Add-level style dropdown excludes styles the student already has', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [
        { danceStyle: 'SALSA', level: 'INTERMEDIATE' },
        { danceStyle: 'BACHATA', level: 'BEGINNER' },
      ],
    }));

    drive(component).addRow();
    const options = drive(component).availableStylesFor(2).map(o => o.value);
    expect(options).not.toContain('SALSA');
    expect(options).not.toContain('BACHATA');
    expect(options).toContain('KIZOMBA');
    expect(options.length).toBe(5);
  });

  it('Cancel discards staged changes', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'INTERMEDIATE' }],
    }));

    drive(component).onLevelChange(0, 'ADVANCED');
    drive(component).addRow();
    expect(drive(component).hasValidChanges()).toBe(false); // added row not yet filled
    expect(drive(component).edits().length).toBe(2);

    drive(component).onCancel();
    expect(drive(component).edits()).toEqual([{ danceStyle: 'SALSA', level: 'INTERMEDIATE' }]);
    expect(drive(component).hasValidChanges()).toBe(false);
  });

  it('Save success refreshes the view from the response', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'INTERMEDIATE' }],
    }));

    drive(component).onLevelChange(0, 'ADVANCED');
    drive(component).onSave();

    const req = httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels'));
    // Server trims/normalizes to a different set — UI must reflect the response, not the staged edits.
    req.flush(makeUpdateResult(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'MASTERCLASS' }],
    })));
    fixture.detectChanges();

    expect(drive(component).edits()).toEqual([{ danceStyle: 'SALSA', level: 'MASTERCLASS' }]);
    expect(drive(component).hasValidChanges()).toBe(false);
  });

  it('Save success with autoConfirmedCount=0 shows the plain "Saved." snackbar', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'INTERMEDIATE' }],
    }));
    const snackOpen = vi.spyOn(TestBed.inject(MatSnackBar), 'open');

    drive(component).onLevelChange(0, 'ADVANCED');
    drive(component).onSave();

    httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels'))
      .flush(makeUpdateResult(makeStudent({
        danceLevels: [{ danceStyle: 'SALSA', level: 'ADVANCED' }],
      }), 0));
    fixture.detectChanges();

    expect(snackOpen).toHaveBeenCalledWith('Saved.', 'Close', expect.objectContaining({ duration: 3000 }));
  });

  it('Save success with autoConfirmedCount>0 surfaces the count in the snackbar', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'BEGINNER' }],
    }));
    const snackOpen = vi.spyOn(TestBed.inject(MatSnackBar), 'open');

    drive(component).onLevelChange(0, 'ADVANCED');
    drive(component).onSave();

    httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels'))
      .flush(makeUpdateResult(makeStudent({
        danceLevels: [{ danceStyle: 'SALSA', level: 'ADVANCED' }],
      }), 2));
    fixture.detectChanges();

    expect(snackOpen).toHaveBeenCalledTimes(1);
    const message = snackOpen.mock.calls[0][0] as string;
    expect(message).toContain('2');
    expect(message.toLowerCase()).toContain('auto-confirmed');
  });

  it('shows an error snackbar on save failure and keeps staged edits', () => {
    setup();
    flushStudent(makeStudent({
      danceLevels: [{ danceStyle: 'SALSA', level: 'INTERMEDIATE' }],
    }));
    const snackOpen = vi.spyOn(TestBed.inject(MatSnackBar), 'open');

    drive(component).onLevelChange(0, 'ADVANCED');
    drive(component).onSave();

    httpTesting.expectOne(r => r.url.endsWith('/api/students/1/dance-levels'))
      .flush({ detail: 'boom' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(snackOpen).toHaveBeenCalled();
    expect(drive(component).edits()).toEqual([{ danceStyle: 'SALSA', level: 'ADVANCED' }]);
  });
});
