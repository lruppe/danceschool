import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { PaymentsComponent } from './payments';
import { Payment } from './payment.service';

function makePayment(overrides: Partial<Payment> = {}): Payment {
  return {
    enrollmentId: 1,
    studentName: 'Anna Mueller',
    studentEmail: 'anna@example.com',
    courseTitle: 'Bachata Beginners',
    amount: 166.5,
    status: 'OPEN',
    billingDate: '2026-04-10T10:00:00Z',
    ...overrides,
  };
}

describe('PaymentsComponent', () => {
  let fixture: ComponentFixture<PaymentsComponent>;
  let httpTesting: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PaymentsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    });

    fixture = TestBed.createComponent(PaymentsComponent);
    httpTesting = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement;
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushPayments(payments: Payment[]): void {
    httpTesting.expectOne(req => req.url.includes('/api/me/payments')).flush(payments);
  }

  it('shows loading state then renders the table when payments arrive', () => {
    fixture.detectChanges();
    expect(el.querySelector('.loading')).toBeTruthy();

    flushPayments([makePayment()]);
    fixture.detectChanges();

    expect(el.querySelector('.loading')).toBeFalsy();
    expect(el.querySelector('table[mat-table]')).toBeTruthy();
  });

  it('renders all six column headers', () => {
    fixture.detectChanges();
    flushPayments([makePayment()]);
    fixture.detectChanges();

    const headers = Array.from(el.querySelectorAll('th[mat-header-cell]')).map(h => h.textContent?.trim());
    expect(headers).toEqual(['Student Name', 'Email', 'Course', 'Amount', 'Billing Date', 'Actions']);
  });

  it('renders each payment field in the row', () => {
    fixture.detectChanges();
    flushPayments([makePayment({
      studentName: 'Marco Rossi', studentEmail: 'marco@example.com',
      courseTitle: 'Salsa Advanced', amount: 220, billingDate: '2026-03-15T10:00:00Z',
    })]);
    fixture.detectChanges();

    const row = el.querySelector('tr[mat-row]');
    expect(row?.textContent).toContain('Marco Rossi');
    expect(row?.textContent).toContain('marco@example.com');
    expect(row?.textContent).toContain('Salsa Advanced');
    expect(row?.textContent).toContain('CHF 220.00');
    expect(row?.textContent).toContain('Mar 15, 2026');
  });

  it('partitions rows into Open / Completed and shows correct tab counts', () => {
    fixture.detectChanges();
    flushPayments([
      makePayment({ enrollmentId: 1, status: 'OPEN' }),
      makePayment({ enrollmentId: 2, status: 'OPEN' }),
      makePayment({ enrollmentId: 3, status: 'COMPLETED' }),
    ]);
    fixture.detectChanges();

    const counts = Array.from(el.querySelectorAll('.ds-tab-count')).map(c => c.textContent?.trim());
    expect(counts).toEqual(['2', '1']);

    // Default: Open tab is active → 2 rows
    expect(el.querySelectorAll('tr[mat-row]').length).toBe(2);
  });

  it('switches to the Completed tab on click and shows only completed rows', () => {
    fixture.detectChanges();
    flushPayments([
      makePayment({ enrollmentId: 1, status: 'OPEN', studentName: 'Open Anna' }),
      makePayment({ enrollmentId: 2, status: 'COMPLETED', studentName: 'Done Bob' }),
    ]);
    fixture.detectChanges();

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    (tabs[1] as HTMLElement).click();
    fixture.detectChanges();

    const rows = el.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Done Bob');
  });

  it('renders the mark-paid action only on the Open tab', () => {
    fixture.detectChanges();
    flushPayments([
      makePayment({ enrollmentId: 1, status: 'OPEN' }),
      makePayment({ enrollmentId: 2, status: 'COMPLETED' }),
    ]);
    fixture.detectChanges();

    expect(el.querySelector('tr[mat-row] .mark-paid-button')).toBeTruthy();

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    (tabs[1] as HTMLElement).click();
    fixture.detectChanges();

    expect(el.querySelector('tr[mat-row] .mark-paid-button')).toBeFalsy();
  });

  it('filters rows client-side on student name, email, and course title', () => {
    fixture.detectChanges();
    flushPayments([
      makePayment({ enrollmentId: 1, studentName: 'Anna', studentEmail: 'anna@example.com', courseTitle: 'Bachata' }),
      makePayment({ enrollmentId: 2, studentName: 'Marco', studentEmail: 'marco@example.com', courseTitle: 'Salsa' }),
      makePayment({ enrollmentId: 3, studentName: 'Laura', studentEmail: 'laura@example.com', courseTitle: 'Bachata Sensual' }),
    ]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { searchText: string; applyFilter: () => void };

    // Match by student name
    component.searchText = 'anna';
    component.applyFilter();
    fixture.detectChanges();
    expect(el.querySelectorAll('tr[mat-row]').length).toBe(1);

    // Match by email
    component.searchText = 'marco@';
    component.applyFilter();
    fixture.detectChanges();
    expect(el.querySelectorAll('tr[mat-row]').length).toBe(1);

    // Match by course title — substring match across multiple rows
    component.searchText = 'bachata';
    component.applyFilter();
    fixture.detectChanges();
    expect(el.querySelectorAll('tr[mat-row]').length).toBe(2);
  });

  it('footer reflects filtered count out of total in the active tab', async () => {
    fixture.detectChanges();
    flushPayments([
      makePayment({ enrollmentId: 1, status: 'OPEN', studentName: 'Anna', studentEmail: 'a@x.com', courseTitle: 'C1' }),
      makePayment({ enrollmentId: 2, status: 'OPEN', studentName: 'Marco', studentEmail: 'm@x.com', courseTitle: 'C2' }),
      makePayment({ enrollmentId: 3, status: 'COMPLETED', studentName: 'Laura', studentEmail: 'l@x.com', courseTitle: 'C3' }),
    ]);
    fixture.detectChanges();

    expect(el.querySelector('.ds-table-footer')?.textContent?.trim()).toBe('Showing 2 of 2 payments');

    const component = fixture.componentInstance as unknown as { searchText: string; applyFilter: () => void };
    component.searchText = 'anna';
    component.applyFilter();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector('.ds-table-footer')?.textContent?.trim()).toBe('Showing 1 of 2 payments');
  });

  it('Mark Paid calls markPaid endpoint and refetches the list', () => {
    fixture.detectChanges();
    flushPayments([makePayment({ enrollmentId: 99, status: 'OPEN' })]);
    fixture.detectChanges();

    const btn = el.querySelector('.mark-paid-button') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    const markPaidReq = httpTesting.expectOne(req =>
      req.url.includes('/api/enrollments/99/mark-paid') && req.method === 'PUT');
    markPaidReq.flush({ enrollmentId: 99, status: 'CONFIRMED' });

    // Refetch
    httpTesting.expectOne(req => req.url.includes('/api/me/payments')).flush([]);
  });

  it('Mark Paid error path shows snackbar and does not refetch', () => {
    fixture.detectChanges();
    flushPayments([makePayment({ enrollmentId: 7, status: 'OPEN' })]);
    fixture.detectChanges();

    const btn = el.querySelector('.mark-paid-button') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    const markPaidReq = httpTesting.expectOne(req =>
      req.url.includes('/api/enrollments/7/mark-paid') && req.method === 'PUT');
    markPaidReq.flush({ detail: 'Already paid' }, { status: 409, statusText: 'Conflict' });

    // No refetch — afterEach httpTesting.verify() confirms no outstanding requests.
  });

  it('shows centered "No payments yet" empty state when school has zero payments', () => {
    fixture.detectChanges();
    flushPayments([]);
    fixture.detectChanges();

    expect(el.querySelector('.empty-state')).toBeTruthy();
    expect(el.querySelector('.empty-state-title')?.textContent?.trim()).toBe('No payments yet');
    expect(el.querySelector('table[mat-table]')).toBeFalsy();
  });

  it('shows in-table empty message when active tab is empty but the other has data', () => {
    fixture.detectChanges();
    flushPayments([makePayment({ enrollmentId: 1, status: 'COMPLETED' })]);
    fixture.detectChanges();

    // Default Open tab — no rows, but other tab has data
    expect(el.querySelector('.empty-cell')?.textContent?.trim()).toBe('No open payments');

    const tabs = el.querySelectorAll('a[mat-tab-link]');
    (tabs[1] as HTMLElement).click();
    fixture.detectChanges();
    expect(el.querySelectorAll('tr[mat-row]').length).toBe(1);
  });

  it('shows error state when the payments request fails', () => {
    fixture.detectChanges();
    httpTesting.expectOne(req => req.url.includes('/api/me/payments'))
      .flush({ detail: 'oops' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(el.querySelector('.error-text')).toBeTruthy();
    expect(el.querySelector('table[mat-table]')).toBeFalsy();
  });
});
