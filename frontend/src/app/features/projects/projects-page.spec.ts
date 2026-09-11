import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { ProjectsPage } from './projects-page';

describe('Projects forms and API state', () => {
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ProjectsPage], providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ActivatedRoute, useValue: { snapshot: { data: {} } } }] });
    requests = TestBed.inject(HttpTestingController);
  });
  afterEach(() => requests.verify());

  it('shows the empty state only after a successful empty response', async () => {
    const fixture = TestBed.createComponent(ProjectsPage);
    requests.expectOne('/api/projects').flush([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Your first project starts here');
  });
  it('shows a server failure with a retry instead of invented records', async () => {
    const fixture = TestBed.createComponent(ProjectsPage);
    requests.expectOne('/api/projects').flush({ detail: 'Service unavailable' }, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Service unavailable');
    expect(fixture.nativeElement.textContent).not.toContain('Your first project starts here');
    expect(fixture.componentInstance.projects()).toEqual([]);
  });
  it('rejects whitespace-only project names without making a write request', () => {
    const fixture = TestBed.createComponent(ProjectsPage);
    requests.expectOne('/api/projects').flush([]);
    fixture.componentInstance.newProject();
    fixture.componentInstance.form.setValue({ name: '   ', description: '' });
    fixture.componentInstance.save();
    requests.expectNone(request => request.method === 'POST');
    expect(fixture.componentInstance.form.invalid).toBe(true);
  });
  it('saves a validated project and renders the server result', async () => {
    const fixture = TestBed.createComponent(ProjectsPage);
    requests.expectOne('/api/projects').flush([]);
    fixture.componentInstance.newProject();
    fixture.componentInstance.form.setValue({ name: '  Client portal  ', description: 'A useful project.' });
    fixture.componentInstance.save();
    const write = requests.expectOne('/api/projects');
    expect(write.request.body).toEqual({ name: 'Client portal', description: 'A useful project.' });
    expect(write.request.method).toBe('POST');
    write.flush({ id: 'p1', name: 'Client portal', description: 'A useful project.', createdAt: '2026-01-01T12:00:00Z', updatedAt: '2026-01-01T12:00:00Z' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Client portal');
    expect(fixture.componentInstance.showForm()).toBe(false);
  });
});
