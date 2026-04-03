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
    let authReq = req;

    if (environment.useDevLogin) {
      // Session-based auth — browser sends cookie automatically
      authReq = req.clone({ withCredentials: true });
    } else {
      // Firebase JWT — attach Bearer token
      const token = auth.idToken();
      if (token) {
        authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
      }
    }

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
