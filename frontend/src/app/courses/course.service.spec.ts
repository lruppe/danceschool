import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CourseService } from './course.service';
import { environment } from '../../environments/environment';

describe('CourseService', () => {
  let service: CourseService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CourseService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('deleteCourse', () => {
    it('issues DELETE /api/courses/:id and completes on 204', () => {
      let completed = false;
      service.deleteCourse(42).subscribe({ complete: () => (completed = true) });

      const req = httpTesting.expectOne(`${environment.apiUrl}/api/courses/42`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });

      expect(completed).toBe(true);
    });

    it('propagates HTTP errors (e.g. 409 conflict) to the subscriber', () => {
      let errorStatus: number | undefined;
      service.deleteCourse(7).subscribe({
        error: (err) => (errorStatus = err.status),
      });

      const req = httpTesting.expectOne(`${environment.apiUrl}/api/courses/7`);
      req.flush({ detail: 'Course is published' }, { status: 409, statusText: 'Conflict' });

      expect(errorStatus).toBe(409);
    });
  });
});
