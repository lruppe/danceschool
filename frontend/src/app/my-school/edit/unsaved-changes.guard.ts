import { CanDeactivateFn } from '@angular/router';
import { MySchoolEditComponent } from './my-school-edit';

export const unsavedChangesGuard: CanDeactivateFn<MySchoolEditComponent> = (component) => {
  if (component.canDeactivate()) {
    return true;
  }
  return confirm('You have unsaved changes. Are you sure you want to leave?');
};
