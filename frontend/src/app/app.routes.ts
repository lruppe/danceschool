import { Routes } from '@angular/router';
import { authGuard } from './shared/auth/auth.guard';
import { unsavedChangesGuard } from './my-school/edit/unsaved-changes.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent) },
  {
    path: '',
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
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
