import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Payment, PaymentService } from './payment.service';
import { EnrollmentService } from '../courses/enrollment.service';
import { extractErrorMessage } from '../shared/error-utils';

interface TabConfig {
  status: 'OPEN' | 'COMPLETED';
  label: string;
  emptyMessage: string;
}

const COLUMNS = ['studentName', 'email', 'course', 'amount', 'billingDate', 'actions'];

const TAB_CONFIGS: TabConfig[] = [
  { status: 'OPEN', label: 'Open', emptyMessage: 'No open payments' },
  { status: 'COMPLETED', label: 'Completed', emptyMessage: 'No completed payments' },
];

const OPEN_TAB_INDEX = 0;
const COMPLETED_TAB_INDEX = 1;

@Component({
  selector: 'app-payments',
  imports: [
    FormsModule,
    MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatSortModule, MatTableModule, MatTabsModule, MatTooltipModule,
  ],
  templateUrl: './payments.html',
  styleUrl: './payments.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentsComponent implements OnInit {
  private paymentService = inject(PaymentService);
  private enrollmentService = inject(EnrollmentService);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  protected tabs = TAB_CONFIGS;
  protected activeTabIndex = signal(OPEN_TAB_INDEX);
  protected loaded = signal(false);
  protected error = signal(false);
  protected hasAnyPayments = signal(false);

  protected tabData: MatTableDataSource<Payment>[] = TAB_CONFIGS.map(() => new MatTableDataSource<Payment>([]));
  protected tabCounts = signal<number[]>(TAB_CONFIGS.map(() => 0));

  protected searchText = '';
  protected columns = COLUMNS;

  protected activeDataSource = computed(() => this.tabData[this.activeTabIndex()]);
  protected activeTab = computed(() => TAB_CONFIGS[this.activeTabIndex()]);
  protected isOpenTab = computed(() => this.activeTabIndex() === OPEN_TAB_INDEX);

  @ViewChild(MatSort) set sort(sort: MatSort) {
    if (sort) {
      this.tabData[this.activeTabIndex()].sort = sort;
    }
  }

  ngOnInit(): void {
    this.tabData.forEach(ds => {
      ds.filterPredicate = this.createFilterPredicate();
      ds.sortingDataAccessor = (payment, column) => {
        switch (column) {
          case 'studentName': return payment.studentName;
          case 'email': return payment.studentEmail;
          case 'course': return payment.courseTitle;
          case 'amount': return payment.amount;
          case 'billingDate': return payment.billingDate;
          default: return '';
        }
      };
    });

    this.loadPayments();
  }

  protected selectTab(index: number): void {
    this.activeTabIndex.set(index);
  }

  protected applyFilter(): void {
    const text = this.searchText.trim().toLowerCase();
    this.tabData.forEach(ds => ds.filter = text);
  }

  protected onMarkPaid(enrollmentId: number): void {
    this.enrollmentService.markPaid(enrollmentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.snackBar.open('Payment confirmed', 'Close', { duration: 3000, panelClass: 'snackbar-success' });
        this.loadPayments();
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(extractErrorMessage(err, 'Failed to confirm payment'), 'Close',
          { duration: 5000, panelClass: 'snackbar-error' });
      },
    });
  }

  protected formatAmount(amount: number): string {
    return `CHF ${amount.toFixed(2)}`;
  }

  protected formatBillingDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  private loadPayments(): void {
    this.paymentService.getPayments().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (payments) => {
        this.partition(payments);
        this.hasAnyPayments.set(payments.length > 0);
        this.loaded.set(true);
      },
      error: () => {
        this.error.set(true);
        this.loaded.set(true);
      },
    });
  }

  private partition(payments: Payment[]): void {
    const open: Payment[] = [];
    const completed: Payment[] = [];
    for (const p of payments) {
      if (p.status === 'OPEN') open.push(p);
      else completed.push(p);
    }
    this.tabData[OPEN_TAB_INDEX].data = open;
    this.tabData[COMPLETED_TAB_INDEX].data = completed;
    this.tabCounts.set([open.length, completed.length]);
  }

  private createFilterPredicate(): (data: Payment, filter: string) => boolean {
    return (data: Payment, filter: string): boolean => {
      if (!filter) return true;
      const searchable = [data.studentName, data.studentEmail, data.courseTitle].join(' ').toLowerCase();
      return searchable.includes(filter);
    };
  }
}
