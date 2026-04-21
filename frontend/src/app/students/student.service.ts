import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface StudentListItem {
  id: number;
  name: string;
  email: string;
  phoneNumber: string | null;
  activeCoursesCount: number;
}

export interface StudentDanceLevel {
  danceStyle: string;
  level: string;
}

export interface StudentDetail {
  id: number;
  name: string;
  email: string;
  phoneNumber: string | null;
  danceLevels: StudentDanceLevel[];
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private http = inject(HttpClient);

  getStudents(): Observable<StudentListItem[]> {
    return this.http.get<StudentListItem[]>(`${environment.apiUrl}/api/students`);
  }

  getStudent(id: number): Observable<StudentDetail> {
    return this.http.get<StudentDetail>(`${environment.apiUrl}/api/students/${id}`);
  }
}
