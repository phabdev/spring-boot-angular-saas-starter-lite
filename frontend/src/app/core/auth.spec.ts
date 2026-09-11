import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { Session } from './api.models';

const session: Session = { accessToken: 'access-one', tokenType: 'Bearer', expiresIn: 900, user: { id: 'owner', email: 'owner@example.test', displayName: 'Owner', role: 'USER' } };
describe('Browser session and authentication interceptor', () => {
  let auth: AuthService;
  let http: HttpClient;
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])] });
    auth = TestBed.inject(AuthService); http = TestBed.inject(HttpClient); requests = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });
  afterEach(() => { requests.verify(); vi.restoreAllMocks(); });
  function signIn() {
    auth.login({ email: session.user.email, password: 'test-password-123' }).subscribe();
    const request = requests.expectOne('/api/auth/login');
    expect(request.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    expect(request.request.withCredentials).toBe(true);
    request.flush(session);
  }

  it('keeps access credentials in memory without browser storage writes', () => {
    const storage = vi.spyOn(Storage.prototype, 'setItem');
    signIn();
    expect(auth.accessToken).toBe('access-one');
    expect(auth.user()?.email).toBe(session.user.email);
    expect(storage).not.toHaveBeenCalled();
  });
  it('restores a reload via the refresh cookie and verifies /me', () => {
    let name = '';
    auth.restore().subscribe(user => name = user.displayName);
    const refresh = requests.expectOne('/api/auth/refresh');
    expect(refresh.request.withCredentials).toBe(true);
    refresh.flush(session);
    const me = requests.expectOne('/api/me');
    expect(me.request.headers.get('Authorization')).toBe('Bearer access-one');
    me.flush(session.user);
    expect(name).toBe('Owner');
  });
  it('shares one refresh across concurrent subscribers', () => {
    const result: string[] = [];
    auth.refresh().subscribe(value => result.push(value.accessToken));
    auth.refresh().subscribe(value => result.push(value.accessToken));
    requests.expectOne('/api/auth/refresh').flush(session);
    expect(result).toEqual(['access-one', 'access-one']);
  });
  it('clears an invalid refresh session and permits a later new attempt', () => {
    signIn();
    auth.refresh().subscribe({ error: () => {} });
    requests.expectOne('/api/auth/refresh').flush({ detail: 'Expired' }, { status: 401, statusText: 'Unauthorized' });
    expect(auth.accessToken).toBeNull();
    expect(auth.user()).toBeNull();
    auth.refresh().subscribe();
    requests.expectOne('/api/auth/refresh').flush(session);
    expect(auth.accessToken).toBe('access-one');
  });
  it('uses the server lifetime to refresh before issuing an expired request', () => {
    vi.spyOn(Date, 'now').mockReturnValue(1000);
    signIn();
    vi.spyOn(Date, 'now').mockReturnValue(902000);
    http.get('/api/projects').subscribe();
    requests.expectOne('/api/auth/refresh').flush({ ...session, accessToken: 'renewed' });
    const projects = requests.expectOne('/api/projects');
    expect(projects.request.headers.get('Authorization')).toBe('Bearer renewed');
    projects.flush([]);
  });
  it('coordinates parallel 401 responses and retries each with the rotated token', () => {
    signIn();
    http.get('/api/projects').subscribe();
    http.get('/api/permissions').subscribe();
    requests.expectOne('/api/projects').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/permissions').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/auth/refresh').flush({ ...session, accessToken: 'access-two' });
    for (const url of ['/api/projects', '/api/permissions']) {
      const retry = requests.expectOne(url);
      expect(retry.request.headers.get('Authorization')).toBe('Bearer access-two');
      retry.flush([]);
    }
  });
  it('does not rotate again for a late 401 sent with the previous token', () => {
    signIn();
    http.get('/api/projects').subscribe();
    http.get('/api/settings').subscribe();
    const late = requests.expectOne('/api/settings');
    requests.expectOne('/api/projects').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/auth/refresh').flush({ ...session, accessToken: 'access-two' });
    requests.expectOne('/api/projects').flush([]);
    late.flush({}, { status: 401, statusText: 'Unauthorized' });
    const retry = requests.expectOne('/api/settings');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer access-two');
    retry.flush({});
  });
  it('retries a 401 once, then signs out without an infinite refresh loop', () => {
    signIn();
    http.get('/api/projects').subscribe({ error: () => {} });
    requests.expectOne('/api/projects').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/auth/refresh').flush(session);
    requests.expectOne('/api/projects').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectNone('/api/auth/refresh');
    expect(auth.user()).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expired: '1' } });
  });
  it('does not attach credentials to third-party requests', () => {
    signIn();
    http.get('https://example.test/api/projects').subscribe();
    const request = requests.expectOne('https://example.test/api/projects');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush([]);
  });
  it('revokes the cookie on logout and clears memory', () => {
    signIn();
    auth.logout().subscribe();
    expect(auth.accessToken).toBeNull();
    const request = requests.expectOne('/api/auth/logout');
    expect(request.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    request.flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.user()).toBeNull();
  });
  it('cannot resurrect a session when refresh completes during logout', () => {
    signIn();
    auth.refresh().subscribe({ error: () => {} });
    auth.logout().subscribe();
    requests.expectNone('/api/auth/logout');
    requests.expectOne('/api/auth/refresh').flush({ ...session, accessToken: 'late-token' });
    requests.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.accessToken).toBeNull();
  });
  it('cannot restore a late login after logout has started', () => {
    auth.login({ email: session.user.email, password: 'test-password-123' }).subscribe({ error: () => {} });
    const login = requests.expectOne('/api/auth/login');
    auth.logout().subscribe();
    requests.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    login.flush(session);
    expect(auth.accessToken).toBeNull();
    expect(auth.user()).toBeNull();
  });
  it('uses a shared Web Lock for cookie-changing requests where available', async () => {
    const lock = vi.fn((_name: string, operation: () => Promise<Session>) => operation());
    const original = Object.getOwnPropertyDescriptor(navigator, 'locks');
    Object.defineProperty(navigator, 'locks', { value: { request: lock }, configurable: true });
    try {
      let result = '';
      auth.refresh().subscribe(value => result = value.accessToken);
      requests.expectOne('/api/auth/refresh').flush(session);
      await Promise.resolve();
      expect(result).toBe('access-one');
      expect(lock).toHaveBeenCalledWith('phabdev-auth-session', expect.any(Function));
    } finally {
      if (original) Object.defineProperty(navigator, 'locks', original);
      else Reflect.deleteProperty(navigator, 'locks');
    }
  });
});
