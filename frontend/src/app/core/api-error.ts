import { HttpErrorResponse } from '@angular/common/http';
export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Cannot reach the server. Check your connection and try again.';
    if (error.status === 429) return 'Too many requests. Please wait and try again.';
    if (typeof error.error?.detail === 'string') return error.error.detail;
    if (error.status === 401) return 'Your session has expired. Please sign in again.';
    if (error.status === 403) return 'You do not have permission to perform this action.';
  }
  return 'The request could not be completed. Please try again.';
}
