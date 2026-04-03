import { Injectable, signal, computed, inject, NgZone } from '@angular/core';
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
  private zone = inject(NgZone);

  private readonly _user = signal<User | null>(null);
  private readonly _loading = signal(true);
  private readonly _checked = signal(false);
  private readonly _idToken = signal<string | null>(null);
  private readonly _loginError = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly isLoading = this._loading.asReadonly();
  readonly isChecked = this._checked.asReadonly();
  readonly idToken = this._idToken.asReadonly();
  readonly loginError = this._loginError.asReadonly();

  constructor() {
    if (environment.useDevLogin) {
      this.initDev();
    } else {
      this.initFirebase();
    }
  }

  // ---------------------------------------------------------------------------
  // Dev mode — form login with session cookie
  // ---------------------------------------------------------------------------

  private initDev(): void {
    // Check if we already have a session (page refresh)
    this.fetchUser();
  }

  async devLogin(email: string, password: string): Promise<void> {
    this._loginError.set(null);
    this._loading.set(true);
    try {
      const body = new URLSearchParams();
      body.set('username', email);
      body.set('password', password);

      await fetch(this.apiUrl('/api/auth/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
        credentials: 'include',
      });

      this.fetchUser();
    } catch {
      this._loading.set(false);
      this._loginError.set('Login failed');
    }
  }

  // ---------------------------------------------------------------------------
  // Production mode — Firebase Google sign-in
  // ---------------------------------------------------------------------------

  private firebaseAuth: import('firebase/auth').Auth | null = null;

  private async initFirebase(): Promise<void> {
    const { initializeApp } = await import('firebase/app');
    const { getAuth, onAuthStateChanged, onIdTokenChanged, connectAuthEmulator } =
      await import('firebase/auth');

    const app = initializeApp(environment.firebase);
    this.firebaseAuth = getAuth(app);

    if (environment.useEmulators) {
      connectAuthEmulator(this.firebaseAuth, 'http://localhost:9099', { disableWarnings: true });
    }

    onAuthStateChanged(this.firebaseAuth, (firebaseUser) => {
      this.zone.run(async () => {
        if (firebaseUser) {
          const token = await firebaseUser.getIdToken();
          this._idToken.set(token);
          this.fetchUser();
        } else {
          this._user.set(null);
          this._idToken.set(null);
          this._checked.set(true);
          this._loading.set(false);
        }
      });
    });

    onIdTokenChanged(this.firebaseAuth, (firebaseUser) => {
      if (firebaseUser) {
        firebaseUser.getIdToken().then(token => {
          this.zone.run(() => this._idToken.set(token));
        });
      }
    });
  }

  async signInWithGoogle(): Promise<void> {
    if (!this.firebaseAuth) return;
    const { signInWithPopup, GoogleAuthProvider } = await import('firebase/auth');
    this._loginError.set(null);
    this._loading.set(true);
    try {
      await signInWithPopup(this.firebaseAuth, new GoogleAuthProvider());
    } catch (error: unknown) {
      this._loading.set(false);
      const message = error instanceof Error ? error.message : 'Sign-in failed';
      this._loginError.set(message);
    }
  }

  // ---------------------------------------------------------------------------
  // Shared
  // ---------------------------------------------------------------------------

  private apiUrl(path: string): string {
    return `${environment.apiUrl}${path}`;
  }

  private fetchUser(): void {
    this.http.get<User>(this.apiUrl('/api/auth/me')).subscribe({
      next: (user) => {
        this._user.set(user);
        this._loading.set(false);
        this._checked.set(true);
      },
      error: () => {
        this._user.set(null);
        this._idToken.set(null);
        this._loading.set(false);
        this._checked.set(true);
      },
    });
  }

  checkAuth(): void {
    this._loading.set(true);
    this.fetchUser();
  }

  async logout(): Promise<void> {
    if (environment.useDevLogin) {
      await fetch(this.apiUrl('/api/auth/logout'), { method: 'POST', credentials: 'include' });
      this._user.set(null);
      this._checked.set(true);
      this._loading.set(false);
      this.router.navigate(['/login']);
    } else if (this.firebaseAuth) {
      const { signOut } = await import('firebase/auth');
      await signOut(this.firebaseAuth);
      this.router.navigate(['/login']);
    }
  }
}
