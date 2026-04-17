import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type EnrollmentStatus = 'PENDING_APPROVAL' | 'PENDING_PAYMENT' | 'CONFIRMED' | 'WAITLISTED' | 'REJECTED';
export type DanceRole = 'LEAD' | 'FOLLOW';
export type WaitlistReason = 'CAPACITY' | 'ROLE_IMBALANCE';

export interface EnrollmentListItem {
  id: number;
  studentName: string;
  studentEmail: string;
  studentPhoneNumber: string | null;
  danceRole: DanceRole | null;
  status: EnrollmentStatus;
  enrolledAt: string;
  approvedAt: string | null;
  paidAt: string | null;
  waitlistPosition: number | null;
  waitlistReason: WaitlistReason | null;
}

export interface EnrollmentResponse {
  enrollmentId: number;
  status: EnrollmentStatus;
}

export interface EnrollStudentRequest {
  studentId: number;
  danceRole?: DanceRole;
}

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private http = inject(HttpClient);

  enrollStudent(courseId: number, dto: EnrollStudentRequest): Observable<EnrollmentResponse> {
    return this.http.post<EnrollmentResponse>(`${environment.apiUrl}/api/courses/${courseId}/enrollments`, dto);
  }

  getEnrollments(courseId: number): Observable<EnrollmentListItem[]> {
    return this.http.get<EnrollmentListItem[]>(`${environment.apiUrl}/api/courses/${courseId}/enrollments`);
  }

  markPaid(enrollmentId: number): Observable<EnrollmentResponse> {
    return this.http.put<EnrollmentResponse>(`${environment.apiUrl}/api/enrollments/${enrollmentId}/mark-paid`, null);
  }
}
