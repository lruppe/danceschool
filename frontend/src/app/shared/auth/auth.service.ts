import { Injectable, signal, computed, inject, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { initializeApp, FirebaseApp } from 'firebase/app';
import {
  getAuth,
  Auth,
  onAuthStateChanged,
  onIdTokenChanged,
  signInWithPopup,
  signOut,
  GoogleAuthProvider,
  connectAuthEmulator,
} from 'firebase/auth';

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

  private app: FirebaseApp;
  private firebaseAuth: Auth;

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
    this.app = initializeApp(environment.firebase);
    this.firebaseAuth = getAuth(this.app);

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

  async signInWithGoogle(): Promise<void> {
    this._loginError.set(null);
    this._loading.set(true);
    try {
      await signInWithPopup(this.firebaseAuth, new GoogleAuthProvider());
      // onAuthStateChanged handles the rest
    } catch (error: unknown) {
      this._loading.set(false);
      const message = error instanceof Error ? error.message : 'Sign-in failed';
      this._loginError.set(message);
    }
  }

  logout(): void {
    signOut(this.firebaseAuth).then(() => {
      // onAuthStateChanged handles clearing state
      this.router.navigate(['/login']);
    });
  }
}
