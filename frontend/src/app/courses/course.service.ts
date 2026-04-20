import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CourseLevel, DanceStyle } from '../shared/course-constants';

export interface CourseListItem {
  id: number;
  title: string;
  danceStyle: DanceStyle;
  level: CourseLevel;
  courseType: 'PARTNER' | 'SOLO';
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  numberOfSessions: number;
  startDate: string;
  endDate: string;
  enrolledStudents: number;
  leadCount: number;
  followCount: number;
  maxParticipants: number;
  price: number;
  status: string;
}

export type CourseEditTier = 'FULLY_EDITABLE' | 'RESTRICTED' | 'READ_ONLY';

export interface CourseDetail {
  id: number;
  title: string;
  danceStyle: DanceStyle;
  level: CourseLevel;
  courseType: string;
  description: string | null;
  startDate: string;
  dayOfWeek: string;
  recurrenceType: string;
  numberOfSessions: number;
  endDate: string;
  startTime: string;
  endTime: string;
  location: string;
  teachers: string | null;
  maxParticipants: number;
  roleBalanceThreshold: number | null;
  priceModel: string;
  price: number;
  status: string;
  publishedAt: string | null;
  enrolledStudents: number;
  editTier: CourseEditTier;
}

@Injectable({ providedIn: 'root' })
export class CourseService {
  private http = inject(HttpClient);

  getCourses(): Observable<CourseListItem[]> {
    return this.http.get<CourseListItem[]>(`${environment.apiUrl}/api/courses/me`);
  }

  getCoursesByStatus(status: string): Observable<CourseListItem[]> {
    return this.http.get<CourseListItem[]>(`${environment.apiUrl}/api/courses/me`, {
      params: { status },
    });
  }

  getCourse(id: number): Observable<CourseDetail> {
    return this.http.get<CourseDetail>(`${environment.apiUrl}/api/courses/${id}`);
  }

  createCourse(dto: Record<string, unknown>): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`${environment.apiUrl}/api/courses`, dto);
  }

  updateCourse(id: number, dto: Record<string, unknown>): Observable<CourseDetail> {
    return this.http.put<CourseDetail>(`${environment.apiUrl}/api/courses/${id}`, dto);
  }

  publishCourse(id: number): Observable<CourseDetail> {
    return this.http.post<CourseDetail>(`${environment.apiUrl}/api/courses/${id}/publish`, null);
  }
}
