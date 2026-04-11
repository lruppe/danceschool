import { FormControl } from '@angular/forms';
import { futureDateValidator } from './course-form.service';

describe('futureDateValidator', () => {
  it('should return null for a future date', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const value = tomorrow.toISOString().split('T')[0];
    const control = new FormControl(value);
    expect(futureDateValidator(control)).toBeNull();
  });

  it('should return error for today', () => {
    const today = new Date().toISOString().split('T')[0];
    const control = new FormControl(today);
    expect(futureDateValidator(control)).toEqual({ futureDate: true });
  });

  it('should return error for a past date', () => {
    const control = new FormControl('2020-01-01');
    expect(futureDateValidator(control)).toEqual({ futureDate: true });
  });

  it('should return null for empty value', () => {
    const control = new FormControl('');
    expect(futureDateValidator(control)).toBeNull();
  });
});
