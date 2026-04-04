import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { PublicHeaderComponent } from '../landing/public-header';
import { PublicFooterComponent } from '../landing/public-footer';

@Component({
  selector: 'app-privacy',
  imports: [PublicHeaderComponent, PublicFooterComponent],
  templateUrl: './privacy.html',
  styleUrl: './legal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyComponent {
  private router = inject(Router);

  navigateHome(): void {
    this.router.navigate(['/']);
  }
}
