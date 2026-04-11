import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CourseLevel, CourseStatus, DanceStyle } from '../shared/course-constants';

export interface CourseListItem {
  id: number;
  title: string;
  danceStyle: DanceStyle;
  level: CourseLevel;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  numberOfSessions: number;
  startDate: string;
  endDate: string;
  enrolledStudents: number;
  maxParticipants: number;
  price: number;
  status: CourseStatus;
  completedSessions: number;
}

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
  roleBalancingEnabled: boolean;
  roleBalanceThreshold: number | null;
  priceModel: string;
  price: number;
  status: CourseStatus;
  publishedAt: string | null;
  enrolledStudents: number;
  completedSessions: number;
}

@Injectable({ providedIn: 'root' })
export class CourseService {
  private http = inject(HttpClient);

  getCourses(): Observable<CourseListItem[]> {
    return this.http.get<CourseListItem[]>(`${environment.apiUrl}/api/courses/me`);
  }

  getCoursesByStatus(status: CourseStatus): Observable<CourseListItem[]> {
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

  updateCourse(id: number, dto: Record<string, unknown>): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/api/courses/${id}`, dto);
  }
}
