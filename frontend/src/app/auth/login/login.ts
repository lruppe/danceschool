import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../shared/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  protected auth = inject(AuthService);
  private router = inject(Router);

  protected isDevLogin = environment.useDevLogin;
  protected email = 'owner@test.com';
  protected password = 'password';

  constructor() {
    effect(() => {
      if (this.auth.isLoggedIn()) {
        const user = this.auth.user();
        if (user && user.memberships.length === 0) {
          this.router.navigate(['/onboarding']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      }
    });
  }

  devLogin(): void {
    this.auth.devLogin(this.email, this.password);
  }

  quickLogin(email: string): void {
    this.email = email;
    this.password = 'password';
    this.auth.devLogin(email, 'password');
  }

  signInWithGoogle(): void {
    this.auth.signInWithGoogle();
  }
}
