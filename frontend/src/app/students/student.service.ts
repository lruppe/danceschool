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

@Injectable({ providedIn: 'root' })
export class StudentService {
  private http = inject(HttpClient);

  getStudents(): Observable<StudentListItem[]> {
    return this.http.get<StudentListItem[]>(`${environment.apiUrl}/api/students`);
  }
}
