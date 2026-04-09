import { CanDeactivateFn } from '@angular/router';

export interface HasUnsavedChanges {
  canDeactivate(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (component.canDeactivate()) {
    return true;
  }
  return confirm('You have unsaved changes. Are you sure you want to leave?');
};
