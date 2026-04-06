import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CourseListItem {
  id: number;
  title: string;
  danceStyle: 'SALSA' | 'BACHATA';
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  numberOfSessions: number;
  enrolledStudents: number;
  maxParticipants: number;
  price: number;
  status: 'DRAFT' | 'ACTIVE' | 'FULL' | 'INACTIVE';
}

@Injectable({ providedIn: 'root' })
export class CourseService {
  private http = inject(HttpClient);

  getCourses(): Observable<CourseListItem[]> {
    return this.http.get<CourseListItem[]>(`${environment.apiUrl}/api/courses/me`);
  }
}
