import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isChecked()) {
    return auth.isLoggedIn() || router.createUrlTree(['/login']);
  }

  // Wait for Firebase to complete initial auth check (session restore from IndexedDB)
  return toObservable(auth.isChecked).pipe(
    filter((checked) => checked),
    take(1),
    map(() => auth.isLoggedIn() || router.createUrlTree(['/login'])),
  );
};
