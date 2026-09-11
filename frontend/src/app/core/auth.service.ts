import { HttpBackend, HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, defer, finalize, firstValueFrom, from, Observable, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Session, User } from './api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Auth calls bypass the interceptor to prevent recursive refresh requests.
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly base = environment.apiUrl;
  private readonly authOptions = { withCredentials: true, headers: { 'X-Requested-With': 'XMLHttpRequest' } };
  private readonly currentUser = signal<User | null>(null);
  readonly user = this.currentUser.asReadonly();
  private token: string | null = null;
  private expiresAt = 0;
  private refreshRequest?: Observable<Session>;
  private epoch = 0;

  get accessToken(): string | null { return this.token; }
  get tokenExpired(): boolean { return Date.now() >= this.expiresAt - 5_000; }

  login(input: { email: string; password: string }): Observable<Session> {
    return this.credentials('/auth/login', input);
  }
  register(input: { email: string; password: string; displayName: string }): Observable<Session> {
    return this.credentials('/auth/register', input);
  }
  refresh(): Observable<Session> {
    if (!this.refreshRequest) {
      const epoch = this.epoch;
      this.refreshRequest = this.authPost<Session>('/auth/refresh', {}).pipe(
        switchMap(session => epoch === this.epoch ? of(session) : throwError(() => new Error('Session changed'))),
        tap(session => this.accept(session)),
        catchError(error => { if (epoch === this.epoch) this.clear(); return throwError(() => error); }),
        finalize(() => { this.refreshRequest = undefined; }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.refreshRequest;
  }
  restore(): Observable<User> {
    const ready: Observable<unknown> = this.token && !this.tokenExpired ? of(null) : this.refresh();
    return ready.pipe(switchMap(() => this.me()), tap(user => this.currentUser.set(user)));
  }
  private me(): Observable<User> {
    return this.http.get<User>(this.base + '/me', { headers: new HttpHeaders({ Authorization: 'Bearer ' + this.token }) });
  }
  logout(): Observable<unknown> {
    // Finish an existing rotation before clearing its cookie on the server.
    const pending: Observable<unknown> = this.refreshRequest ?? of(null);
    this.epoch++;
    this.clear();
    return pending.pipe(
      catchError(() => of(null)),
      switchMap(() => this.authPost('/auth/logout', {})),
      finalize(() => this.clear())
    );
  }
  clear(): void { this.token = null; this.expiresAt = 0; this.currentUser.set(null); }
  private credentials(path: string, input: unknown): Observable<Session> {
    const epoch = ++this.epoch;
    return this.authPost<Session>(path, input).pipe(
      switchMap(session => epoch === this.epoch ? of(session) : throwError(() => new Error('Session changed'))),
      tap(session => this.accept(session))
    );
  }
  private authPost<T>(path: string, input: unknown): Observable<T> {
    return defer(() => {
      const request = this.http.post<T>(this.base + path, input, this.authOptions);
      // Cookies are shared across tabs. Serialize rotations and session mutations
      // on localhost/HTTPS so another tab cannot replay a just-consumed token.
      if (typeof navigator !== 'undefined' && navigator.locks) {
        return from(navigator.locks.request('phabdev-auth-session', () => firstValueFrom(request)));
      }
      return request;
    });
  }
  private accept(session: Session): void {
    this.token = session.accessToken;
    this.expiresAt = Date.now() + session.expiresIn * 1000;
    this.currentUser.set(session.user);
  }
}
