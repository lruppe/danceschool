import { Routes } from '@angular/router';
import { authGuard } from './shared/auth/auth.guard';
import { publicGuard } from './shared/auth/public.guard';
import { unsavedChangesGuard } from './shared/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [publicGuard],
    loadComponent: () => import('./landing/landing').then(m => m.LandingComponent),
  },
  { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent) },
  { path: 'terms', loadComponent: () => import('./legal/terms').then(m => m.TermsComponent) },
  { path: 'privacy', loadComponent: () => import('./legal/privacy').then(m => m.PrivacyComponent) },
  {
    path: 'app',
    loadComponent: () => import('./shell/shell').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard').then(m => m.DashboardComponent) },
      { path: 'students', loadComponent: () => import('./students/students').then(m => m.StudentsComponent) },
      {
        path: 'my-school/edit',
        loadComponent: () => import('./my-school/edit/my-school-edit').then(m => m.MySchoolEditComponent),
        canDeactivate: [unsavedChangesGuard],
      },
      { path: 'my-school', loadComponent: () => import('./my-school/my-school').then(m => m.MySchoolComponent) },
      { path: 'subscriptions', loadComponent: () => import('./subscriptions/subscriptions').then(m => m.SubscriptionsComponent) },
      {
        path: 'courses/create',
        loadComponent: () => import('./courses/create/course-create').then(m => m.CourseCreateComponent),
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'courses/:id/edit',
        loadComponent: () => import('./courses/create/course-create').then(m => m.CourseCreateComponent),
        canDeactivate: [unsavedChangesGuard],
      },
      { path: 'courses', loadComponent: () => import('./courses/courses').then(m => m.CoursesComponent) },
      { path: 'payments', loadComponent: () => import('./payments/payments').then(m => m.PaymentsComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
