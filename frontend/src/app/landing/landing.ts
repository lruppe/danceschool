import { ChangeDetectionStrategy, Component, ElementRef, inject, viewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { PublicHeaderComponent } from './public-header';
import { PublicFooterComponent } from './public-footer';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, PublicHeaderComponent, PublicFooterComponent],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingComponent {
  private pageTop = viewChild<ElementRef>('pageTop');

  constructor() {
    inject(Title).setTitle('DanceStudio — Simplify Your Studio, Amplify Your Passion');
  }

  scrollToTop(): void {
    this.pageTop()?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }

  scrollToFeatures(): void {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
  }
}
