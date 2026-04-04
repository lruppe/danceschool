import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormControl, FormGroup } from '@angular/forms';
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
    ReactiveFormsModule,
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
  protected loginForm = new FormGroup({
    email: new FormControl('owner@test.com', { nonNullable: true }),
    password: new FormControl('password', { nonNullable: true }),
  });

  constructor() {
    effect(() => {
      if (this.auth.isLoggedIn()) {
        this.router.navigate(['/app/dashboard']);
      }
    });
  }

  devLogin(): void {
    const { email, password } = this.loginForm.getRawValue();
    this.auth.devLogin(email, password);
  }

  quickLogin(email: string): void {
    this.loginForm.setValue({ email, password: 'password' });
    this.auth.devLogin(email, 'password');
  }

  signInWithGoogle(): void {
    this.auth.signInWithGoogle();
  }
}
