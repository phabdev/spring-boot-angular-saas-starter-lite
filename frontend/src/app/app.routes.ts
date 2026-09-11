import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage) },
  { path: 'register', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { register: true } },
  { path: '', loadComponent: () => import('./features/layout/shell').then(m => m.Shell), canActivate: [authGuard], canActivateChild: [authGuard], children: [
    { path: 'dashboard', loadComponent: () => import('./features/projects/projects-page').then(m => m.ProjectsPage), data: { dashboard: true } },
    { path: 'projects', loadComponent: () => import('./features/projects/projects-page').then(m => m.ProjectsPage) },
    { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
  ] },
  { path: '**', redirectTo: 'dashboard' }
];
