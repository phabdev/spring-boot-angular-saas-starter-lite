import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs';
import { apiError } from '../../core/api-error';
import { Project } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { ProjectsService } from '../../core/projects.service';

@Component({
  selector: 'app-projects-page',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressBarModule],
  template: `
    <div class="page-heading"><div><p class="eyebrow">YOUR WORKSPACE</p><h1>{{ dashboard ? 'Welcome, ' + auth.user()?.displayName : 'Projects' }}</h1>
      <p class="muted">{{ dashboard ? 'A place to organize what you are building.' : 'Create, edit and organize your projects.' }}</p></div>
      <button mat-flat-button (click)="newProject()">New project</button></div>
    @if (dashboard && !loading() && !loadError()) {
      <section class="overview"><mat-card><mat-card-content><span class="muted">Your projects</span><strong class="metric">{{ projects().length }}</strong><span>Projects you own in this workspace</span></mat-card-content></mat-card>
        <mat-card><mat-card-content><span class="muted">Signed in as</span><strong class="identity">{{ auth.user()?.email }}</strong><span>{{ auth.user()?.role === 'ADMIN' ? 'Administrator' : 'Member' }}</span></mat-card-content></mat-card></section>
    }
    @if (showForm()) {
      <mat-card class="project-form"><mat-card-content><h2>{{ editingId() ? 'Edit project' : 'New project' }}</h2>
        <form [formGroup]="form" (ngSubmit)="save()">
          <mat-form-field appearance="outline"><mat-label>Project name</mat-label><input matInput formControlName="name" maxlength="120"><mat-error>Enter a name, up to 120 characters.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Description</mat-label><textarea matInput formControlName="description" rows="3" maxlength="2000"></textarea><mat-hint>Optional · up to 2,000 characters</mat-hint><mat-error>Description is too long.</mat-error></mat-form-field>
          @if (saveError()) { <p class="error" role="alert">{{ saveError() }}</p> }
          <div class="actions"><button mat-flat-button type="submit" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save project' }}</button><button mat-button type="button" [disabled]="saving()" (click)="showForm.set(false)">Cancel</button></div>
        </form></mat-card-content></mat-card>
    }
    <section class="list-section" aria-label="Projects">
      <div class="section-heading"><h2>{{ dashboard ? 'Your projects' : 'All projects' }}</h2><button mat-button (click)="load()" [disabled]="loading()">Refresh</button></div>
      @if (loading()) { <mat-progress-bar mode="indeterminate" aria-label="Loading projects" /><p class="muted" role="status">Loading your projects…</p> }
      @else if (loadError()) { <div class="empty-state"><h3>Projects could not be loaded</h3><p class="error" role="alert">{{ loadError() }}</p><button mat-stroked-button (click)="load()">Try again</button></div> }
      @else if (projects().length === 0) { <div class="empty-state"><div class="empty-mark">P</div><h3>Your first project starts here</h3><p>Create a project to give your next idea a place.</p><button mat-stroked-button (click)="newProject()">Create project</button></div> }
      @else {
        <div class="project-grid">@for (project of projects(); track project.id) {
          <mat-card class="project-card"><mat-card-content><span class="card-label">PROJECT</span><h3>{{ project.name }}</h3><p class="project-description">{{ project.description || 'No description added.' }}</p><p class="muted small">Updated {{ project.updatedAt | date:'mediumDate' }}</p></mat-card-content>
            <mat-card-actions><button mat-button [disabled]="saving() || deletingId() !== null" (click)="edit(project)">Edit</button><button mat-button [disabled]="deletingId() !== null" (click)="confirmDelete.set(project.id)">Delete</button></mat-card-actions>
            @if (confirmDelete() === project.id) { <div class="delete-confirm" role="group" aria-label="Confirm deletion"><p>Delete “{{ project.name }}”? This cannot be undone.</p><button mat-flat-button [disabled]="deletingId() !== null" (click)="remove(project)">{{ deletingId() === project.id ? 'Deleting…' : 'Delete project' }}</button><button mat-button [disabled]="deletingId() !== null" (click)="confirmDelete.set(null)">Keep project</button></div> }
          </mat-card>
        }</div>
      }
      @if (actionError()) { <p class="error" role="alert">{{ actionError() }}</p> }
      @if (notice()) { <p class="success" role="status">{{ notice() }}</p> }
    </section>
  `
})
export class ProjectsPage {
  private readonly api = inject(ProjectsService);
  readonly auth = inject(AuthService);
  readonly dashboard = inject(ActivatedRoute).snapshot.data['dashboard'] === true;
  readonly projects = signal<Project[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly saveError = signal('');
  readonly actionError = signal('');
  readonly notice = signal('');
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly confirmDelete = signal<string | null>(null);
  readonly deletingId = signal<string | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({ name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(120)]], description: ['', Validators.maxLength(2000)] });
  constructor() { this.load(); }
  load(): void {
    this.loading.set(true); this.loadError.set('');
    this.api.list().pipe(finalize(() => this.loading.set(false))).subscribe({ next: projects => this.projects.set(projects), error: error => this.loadError.set(apiError(error)) });
  }
  newProject(): void { if (this.saving()) return; this.editingId.set(null); this.form.reset(); this.saveError.set(''); this.notice.set(''); this.showForm.set(true); }
  edit(project: Project): void { this.editingId.set(project.id); this.form.setValue({ name: project.name, description: project.description }); this.saveError.set(''); this.notice.set(''); this.showForm.set(true); }
  save(): void {
    if (this.saving()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true); this.saveError.set(''); this.notice.set('');
    const input = this.form.getRawValue(); input.name = input.name.trim();
    const id = this.editingId();
    (id ? this.api.update(id, input) : this.api.create(input)).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: project => { this.projects.update(list => id ? list.map(item => item.id === id ? project : item) : [project, ...list]); this.showForm.set(false); this.notice.set(id ? 'Project updated.' : 'Project created.'); },
      error: error => this.saveError.set(apiError(error))
    });
  }
  remove(project: Project): void {
    if (this.deletingId()) return;
    this.deletingId.set(project.id); this.actionError.set(''); this.notice.set('');
    this.api.delete(project.id).pipe(finalize(() => this.deletingId.set(null))).subscribe({
      next: () => { this.projects.update(list => list.filter(item => item.id !== project.id)); this.confirmDelete.set(null); if (this.editingId() === project.id) this.showForm.set(false); this.notice.set('Project deleted.'); },
      error: error => this.actionError.set(apiError(error))
    });
  }
}
