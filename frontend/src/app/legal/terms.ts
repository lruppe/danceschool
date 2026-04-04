import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { PublicHeaderComponent } from '../landing/public-header';
import { PublicFooterComponent } from '../landing/public-footer';

@Component({
  selector: 'app-terms',
  imports: [PublicHeaderComponent, PublicFooterComponent],
  templateUrl: './terms.html',
  styleUrl: './legal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TermsComponent {
  private router = inject(Router);

  navigateHome(): void {
    this.router.navigate(['/']);
  }
}
