import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

export interface Membership {
  schoolId: number;
  schoolName: string;
  role: 'OWNER' | 'USER';
}

export interface User {
  id: number;
  email: string;
  name: string;
  avatarUrl: string | null;
  memberships: Membership[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly _user = signal<User | null>(null);
  private readonly _loading = signal(true);
  private readonly _checked = signal(false);

  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly isLoading = this._loading.asReadonly();
  readonly isChecked = this._checked.asReadonly();

  private apiUrl(path: string): string {
    return `${environment.apiUrl}${path}`;
  }

  checkAuth(): void {
    this._loading.set(true);
    this.http.get<User>(this.apiUrl('/api/auth/me')).subscribe({
      next: (user) => {
        this._user.set(user);
        this._loading.set(false);
        this._checked.set(true);
      },
      error: () => {
        this._user.set(null);
        this._loading.set(false);
        this._checked.set(true);
      },
    });
  }

  loginWithGoogle(): void {
    window.location.href = `${environment.apiUrl}/oauth2/authorization/google`;
  }

  loginWithGitHub(): void {
    window.location.href = `${environment.apiUrl}/oauth2/authorization/github`;
  }

  logout(): void {
    this.http.post(this.apiUrl('/api/auth/logout'), null).subscribe({
      next: () => {
        this._user.set(null);
        this.router.navigate(['/login']);
      },
      error: () => {
        this._user.set(null);
        this.router.navigate(['/login']);
      },
    });
  }

  devLogin(email: string): void {
    this.http.post(this.apiUrl('/api/dev/login'), { email }).subscribe({
      next: () => this.checkAuth(),
    });
  }
}
