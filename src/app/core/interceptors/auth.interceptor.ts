import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service'; 
import { from, switchMap, catchError, of } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Use 'from' to handle the Promise from getToken()
  return from(authService.getToken()).pipe(
    switchMap(token => {
      if (token) {
        const authReq = req.clone({
          setHeaders: { Authorization: `Bearer ${token}` }
        });
        return next(authReq);
      }
      return next(req);
    }),
    catchError(err => {
      // If getToken fails (e.g., auth not ready yet), just send the request without token
      console.warn('Interceptor: Could not get token', err);
      return next(req);
    })
  );
};