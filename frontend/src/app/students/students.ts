import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../shared/auth/auth.service';

@Component({
  selector: 'app-students',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    @if (hasSchool()) {
      <div class="students-page">
        <h2>Students</h2>
        <p>Manage and view all students enrolled in your dance school</p>
      </div>
    } @else {
      <div class="empty-state">
        <mat-icon class="empty-state-icon">business</mat-icon>
        <h2 class="empty-state-title">Set up your school to get started</h2>
        <p class="empty-state-text">Create your dance school profile to begin managing students.</p>
        <a mat-flat-button routerLink="/app/my-school/edit">Create School</a>
      </div>
    }
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
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: var(--ds-spacing-12) var(--ds-spacing-6);
    }
    .empty-state-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: var(--mat-sys-on-surface-variant);
      margin-bottom: var(--ds-spacing-4);
    }
    .empty-state-title {
      font: var(--mat-sys-headline-small);
      color: var(--mat-sys-on-surface);
      margin-bottom: var(--ds-spacing-2);
    }
    .empty-state-text {
      font: var(--mat-sys-body-large);
      color: var(--mat-sys-on-surface-variant);
      margin-bottom: var(--ds-spacing-6);
      max-width: 400px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentsComponent {
  private auth = inject(AuthService);
  protected hasSchool = computed(() => {
    const u = this.auth.user();
    return u ? u.memberships.length > 0 : false;
  });
}
