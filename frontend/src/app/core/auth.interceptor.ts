import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, Observable, of, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  // Never send application credentials to third-party URLs.
  if (!request.url.startsWith(environment.apiUrl + '/') || request.url.startsWith(environment.apiUrl + '/auth/')) return next(request);
  const auth = inject(AuthService);
  const router = inject(Router);
  const ready: Observable<unknown> = auth.accessToken && auth.tokenExpired ? auth.refresh() : of(null);
  return ready.pipe(switchMap(() => {
    const sentToken = auth.accessToken;
    const authorized = sentToken ? request.clone({ setHeaders: { Authorization: 'Bearer ' + sentToken } }) : request;
    return next(authorized).pipe(catchError(error => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) return throwError(() => error);
      // A concurrent request may already have rotated the token.
      const refreshed: Observable<unknown> = auth.accessToken && auth.accessToken !== sentToken ? of(null) : auth.refresh();
      return refreshed.pipe(switchMap(() => next(request.clone({ setHeaders: { Authorization: 'Bearer ' + auth.accessToken } }))));
    }));
  }), catchError(error => {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      auth.clear();
      void router.navigate(['/login'], { queryParams: { expired: '1' } });
    }
    return throwError(() => error);
  }));
};
