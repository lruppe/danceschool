import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (req.url.startsWith('/api') || req.url.startsWith(environment.apiUrl)) {
    const token = auth.idToken();
    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next(authReq).pipe(
      catchError((error) => {
        if (error.status === 401) {
          router.navigate(['/login']);
        }
        return throwError(() => error);
      }),
    );
  }
  return next(req);
};
