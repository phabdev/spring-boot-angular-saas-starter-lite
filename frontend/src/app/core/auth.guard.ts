import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.restore().pipe(
    map(() => true),
    catchError(() => { auth.clear(); return of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })); })
  );
};
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.restore().pipe(
    map(user => user.role === 'ADMIN' ? true : router.createUrlTree(['/dashboard'])),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
