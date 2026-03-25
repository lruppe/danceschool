import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-students',
  template: `
    <div class="students-page">
      <h2>Students</h2>
      <p>Manage and view all students enrolled in your dance school</p>
    </div>
  `,
  styles: `
    .students-page {
      h2 {
        font: var(--mat-sys-headline-small);
        color: var(--mat-sys-on-surface);
        margin-bottom: var(--ds-spacing-2);
      }
      p {
        font: var(--mat-sys-body-large);
        color: var(--mat-sys-on-surface-variant);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentsComponent {}
