import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-field-help',
  imports: [MatIconModule, MatTooltipModule],
  template: `
    <mat-icon
      class="help-icon"
      [matTooltip]="text()"
      matTooltipPosition="above"
      matTooltipClass="field-help-tooltip"
    >help_outline</mat-icon>
  `,
  styles: `
    .help-icon {
      font-size: var(--ds-spacing-5);
      width: var(--ds-spacing-5);
      height: var(--ds-spacing-5);
      color: var(--mat-sys-on-surface-variant);
      cursor: help;
      vertical-align: middle;
      margin-left: var(--ds-spacing-1);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldHelpComponent {
  text = input.required<string>();
}
