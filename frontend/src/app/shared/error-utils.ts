import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts a user-facing error message from an HTTP error response.
 * Supports RFC 7807 Problem Details (the `detail` field) which our backend
 * returns for domain-rule violations (409), validation errors (400/422), etc.
 */
export function extractErrorMessage(error: HttpErrorResponse, fallback: string): string {
  const detail = error.error?.detail;
  return typeof detail === 'string' && detail.length > 0 ? detail : fallback;
}
