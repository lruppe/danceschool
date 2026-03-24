import { Routes } from '@angular/router';
import { authGuard } from './shared/auth/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent) },
  { path: 'auth/callback', loadComponent: () => import('./auth/callback/callback').then(m => m.AuthCallbackComponent) },
  { path: 'onboarding', loadComponent: () => import('./onboarding/onboarding').then(m => m.OnboardingComponent), canActivate: [authGuard] },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
