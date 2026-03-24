import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/auth/auth.service';

@Component({
  selector: 'app-auth-callback',
  template: `<p>Signing you in...</p>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthCallbackComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  constructor() {
    this.auth.checkAuth();

    effect(() => {
      if (!this.auth.isChecked()) return;

      if (this.auth.isLoggedIn()) {
        const user = this.auth.user();
        if (user && user.memberships.length === 0) {
          this.router.navigate(['/onboarding']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}
