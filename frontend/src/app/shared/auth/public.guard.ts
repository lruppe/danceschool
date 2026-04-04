import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from './auth.service';

/** Redirects logged-in users to /app/dashboard; lets unauthenticated users through. */
export const publicGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isChecked()) {
    return auth.isLoggedIn() ? router.createUrlTree(['/app/dashboard']) : true;
  }

  return toObservable(auth.isChecked).pipe(
    filter((checked) => checked),
    take(1),
    map(() => auth.isLoggedIn() ? router.createUrlTree(['/app/dashboard']) : true),
  );
};
