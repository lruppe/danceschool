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
  endDate: string;
  enrolledStudents: number;
  maxParticipants: number;
  price: number;
  status: CourseStatus;
}

@Injectable({ providedIn: 'root' })
export class CourseService {
  private http = inject(HttpClient);

  getCourses(): Observable<CourseListItem[]> {
    return this.http.get<CourseListItem[]>(`${environment.apiUrl}/api/courses/me`);
  }

  getCourse(id: number): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${environment.apiUrl}/api/courses/${id}`);
  }

  createCourse(dto: Record<string, unknown>): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`${environment.apiUrl}/api/courses`, dto);
  }

  updateCourse(id: number, dto: Record<string, unknown>): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/api/courses/${id}`, dto);
  }
}
