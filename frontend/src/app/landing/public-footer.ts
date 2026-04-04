import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-public-footer',
  imports: [RouterLink],
  template: `
    <footer class="footer">
      <div class="footer-top">
        <div class="logo">
          <span class="logo-icon"></span>
          <span class="logo-text">DanceStudio</span>
        </div>
        <nav class="footer-links">
          <a routerLink="/terms">Terms of Service</a>
          <a routerLink="/privacy">Privacy Policy</a>
          <a href="mailto:supportdanceschool&#64;gmail.com">Contact</a>
        </nav>
      </div>
      <div class="divider"></div>
      <div class="footer-bottom">
        <p class="copyright">&copy; 2026 DanceStudio. All rights reserved.</p>
      </div>
    </footer>
  `,
  styleUrl: './public-footer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicFooterComponent {}
