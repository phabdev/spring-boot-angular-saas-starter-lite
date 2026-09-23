import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { environment } from '../../../environments/environment';
import { apiError } from '../../core/api-error';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatButtonModule],
  template: `
    <div class="app-shell"><aside class="sidebar">
      <a class="brand" routerLink="/dashboard">PhabStarterKit <span>{{ edition }}</span></a>
      <p class="nav-label">WORKSPACE</p><nav aria-label="Main navigation">
        <a routerLink="/dashboard" routerLinkActive="active">Overview</a>
        <a routerLink="/projects" routerLinkActive="active">Projects</a>
      </nav>
      <div class="sidebar-note">by PHABDEV<span>Spring Boot + Angular</span></div>
    </aside><div class="workspace"><header class="topbar"><span class="workspace-label">Personal workspace</span>
      <div class="account"><span>{{ auth.user()?.displayName }}<small>{{ auth.user()?.role }}</small></span><button mat-button [disabled]="signingOut()" (click)="logout()">{{ signingOut() ? 'Signing out…' : 'Sign out' }}</button></div>
    </header><main id="main-content" class="page-content">@if (logoutError()) { <p class="error" role="alert">Sign-out could not be confirmed by the server. {{ logoutError() }} Retry Sign out when your connection is available.</p> }<router-outlet /></main></div></div>
  `
})
export class Shell {
  readonly auth = inject(AuthService);
  readonly edition = environment.edition;
  private readonly router = inject(Router);
  readonly signingOut = signal(false);
  readonly logoutError = signal('');
  logout(): void {
    if (this.signingOut()) return;
    this.signingOut.set(true);
    this.logoutError.set('');
    this.auth.logout().subscribe({
      next: () => void this.router.navigate(['/login']),
      error: error => { this.signingOut.set(false); this.logoutError.set(apiError(error)); }
    });
  }
}
