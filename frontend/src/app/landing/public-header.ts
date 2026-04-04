import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-public-header',
  imports: [RouterLink],
  template: `
    <header class="header">
      <a class="logo" (click)="logoClick.emit()">
        <span class="logo-icon"></span>
        <span class="logo-text">DanceStudio</span>
      </a>
      <div class="header-right">
        <nav class="nav-links">
          <a (click)="featuresClick.emit()">Features</a>
          <a>Pricing</a>
        </nav>
        <div class="header-buttons">
          <a class="btn-login" routerLink="/login">Login</a>
          <a class="btn-get-started" routerLink="/login">Get Started</a>
        </div>
      </div>
    </header>
  `,
  styleUrl: './public-header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicHeaderComponent {
  logoClick = output<void>();
  featuresClick = output<void>();
}
