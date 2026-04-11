import { HttpErrorResponse } from '@angular/common/http';
import { extractErrorMessage } from './error-utils';

describe('extractErrorMessage', () => {
  const fallback = 'Something went wrong';

  it('should return the detail from an RFC 7807 response', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { type: 'about:blank', title: 'Domain Rule Violation', status: 409, detail: 'Start date must be in the future' },
    });
    expect(extractErrorMessage(error, fallback)).toBe('Start date must be in the future');
  });

  it('should return the fallback when there is no detail field', () => {
    const error = new HttpErrorResponse({ status: 500, error: { message: 'Internal error' } });
    expect(extractErrorMessage(error, fallback)).toBe(fallback);
  });

  it('should return the fallback when error body is null', () => {
    const error = new HttpErrorResponse({ status: 0, error: null });
    expect(extractErrorMessage(error, fallback)).toBe(fallback);
  });

  it('should return the fallback when detail is an empty string', () => {
    const error = new HttpErrorResponse({ status: 400, error: { detail: '' } });
    expect(extractErrorMessage(error, fallback)).toBe(fallback);
  });

  it('should return the fallback when detail is not a string', () => {
    const error = new HttpErrorResponse({ status: 400, error: { detail: 42 } });
    expect(extractErrorMessage(error, fallback)).toBe(fallback);
  });

  it('should handle a 400 validation error with detail', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { type: 'about:blank', title: 'Bad Request', status: 400, detail: 'Name is required' },
    });
    expect(extractErrorMessage(error, fallback)).toBe('Name is required');
  });
});
