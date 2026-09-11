import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth.service';
import { apiError } from '../../core/api-error';
import { passwordByteLimit } from '../../core/password.validator';

@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  template: `
    <main class="auth-page">
      <section class="auth-intro"><a class="brand" routerLink="/">PHABDEV <span>{{ edition }}</span></a>
        <p class="eyebrow">YOUR APPLICATION STARTS HERE</p><h1>A clear workspace.<br>A solid foundation.</h1>
        <p>Sign in to manage your projects and continue building.</p>
        <div class="stack-label">SPRING BOOT · ANGULAR · POSTGRESQL</div>
      </section>
      <mat-card class="auth-card"><mat-card-content>
        <p class="eyebrow">SAAS STARTER KIT</p><h2>{{ registerMode ? 'Create your account' : 'Welcome back' }}</h2>
        <p class="muted">{{ registerMode ? 'Start with a personal project workspace.' : 'Enter your account details to continue.' }}</p>
        @if (expired) { <p class="notice" role="status">Your session has expired. Please sign in again.</p> }
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <form [formGroup]="form" (ngSubmit)="submit()">
          @if (registerMode) {
            <mat-form-field appearance="outline"><mat-label>Display name</mat-label><input matInput formControlName="displayName" autocomplete="name" maxlength="100">
              <mat-error>Enter a display name, up to 100 characters.</mat-error></mat-form-field>
          }
          <mat-form-field appearance="outline"><mat-label>Email</mat-label><input matInput type="email" formControlName="email" autocomplete="email" maxlength="254">
            <mat-error>Enter a valid email address.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Password</mat-label><input matInput type="password" formControlName="password" [autocomplete]="registerMode ? 'new-password' : 'current-password'" maxlength="72">
            @if (registerMode) { <mat-hint>Use between 12 and 72 characters.</mat-hint> }
            <mat-error>{{ form.controls.password.hasError('passwordBytes') ? 'Password exceeds 72 UTF-8 bytes. Use fewer characters.' : registerMode ? 'Use between 12 and 72 characters.' : 'Enter your password.' }}</mat-error></mat-form-field>
          <button mat-flat-button type="submit" class="full-width" [disabled]="busy()">{{ busy() ? 'Please wait…' : registerMode ? 'Create account' : 'Sign in' }}</button>
        </form>
        <p class="auth-switch">{{ registerMode ? 'Already have an account?' : 'New here?' }} <a [routerLink]="registerMode ? '/login' : '/register'">{{ registerMode ? 'Sign in' : 'Create an account' }}</a></p>
      </mat-card-content></mat-card>
    </main>
  `
})
export class AuthPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly registerMode = this.route.snapshot.data['register'] === true;
  readonly expired = this.route.snapshot.queryParamMap.has('expired');
  readonly edition = environment.edition;
  readonly busy = signal(false);
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    displayName: ['', this.registerMode ? [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] : []],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', this.registerMode ? [Validators.required, Validators.minLength(12), Validators.maxLength(72), passwordByteLimit] : [Validators.required, Validators.maxLength(72), passwordByteLimit]]
  });
  submit(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true); this.error.set('');
    const input = this.form.getRawValue();
    const request = this.registerMode ? this.auth.register({ ...input, displayName: input.displayName.trim(), email: input.email.trim() }) : this.auth.login({ email: input.email.trim(), password: input.password });
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        const url = this.route.snapshot.queryParamMap.get('returnUrl');
        void this.router.navigateByUrl(url?.startsWith('/') && !url.startsWith('//') && !url.startsWith('/login') ? url : '/dashboard');
      },
      error: error => this.error.set(apiError(error))
    });
  }
}
