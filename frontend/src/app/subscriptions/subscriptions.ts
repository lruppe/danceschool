import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-subscriptions',
  imports: [MatIconModule],
  template: `
    <div class="placeholder">
      <mat-icon class="placeholder-icon">construction</mat-icon>
      <h2>Subscriptions</h2>
      <p>Coming soon!</p>
    </div>
  `,
  styles: `
    .placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: var(--ds-spacing-24);
      color: var(--mat-sys-on-surface-variant);
      text-align: center;
    }
    .placeholder-icon {
      font-size: var(--ds-spacing-12);
      width: var(--ds-spacing-12);
      height: var(--ds-spacing-12);
      margin-bottom: var(--ds-spacing-4);
      opacity: 0.4;
    }
    h2 { font: var(--mat-sys-headline-small); color: var(--mat-sys-on-surface); margin: 0 0 var(--ds-spacing-2); }
    p { font: var(--mat-sys-body-large); margin: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubscriptionsComponent {}
