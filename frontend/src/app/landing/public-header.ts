import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core';
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
      <button
        class="hamburger"
        aria-label="Toggle menu"
        [attr.aria-expanded]="menuOpen()"
        (click)="menuOpen.set(!menuOpen())"
      >
        <span class="hamburger-line"></span>
        <span class="hamburger-line"></span>
        <span class="hamburger-line"></span>
      </button>
    </header>
    @if (menuOpen()) {
      <nav class="mobile-menu">
        <a (click)="featuresClick.emit(); menuOpen.set(false)">Features</a>
        <a>Pricing</a>
        <div class="mobile-menu-divider"></div>
        <a class="btn-login" routerLink="/login">Login</a>
        <a class="btn-get-started" routerLink="/login">Get Started</a>
      </nav>
    }
  `,
  styleUrl: './public-header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicHeaderComponent {
  logoClick = output<void>();
  featuresClick = output<void>();
  menuOpen = signal(false);
}
