import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type PaymentStatus = 'OPEN' | 'COMPLETED';

export interface Payment {
  enrollmentId: number;
  studentName: string;
  studentEmail: string;
  courseTitle: string;
  amount: number;
  status: PaymentStatus;
  billingDate: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);

  getPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${environment.apiUrl}/api/payments/me`);
  }
}
