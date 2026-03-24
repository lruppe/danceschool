import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../shared/auth/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  imports: [MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  protected auth = inject(AuthService);
  protected isDev = !environment.production;

  loginWithGoogle(): void {
    this.auth.loginWithGoogle();
  }

  loginWithGitHub(): void {
    this.auth.loginWithGitHub();
  }

  devLogin(): void {
    this.auth.devLogin('dev@test.com');
  }
}
