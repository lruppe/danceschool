import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../shared/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected auth = inject(AuthService);
  protected user = this.auth.user;

  private breakpointObserver = inject(BreakpointObserver);
  private sidenav = viewChild<MatSidenav>('sidenav');

  protected isDesktop = signal(true);

  protected navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Students', icon: 'people', route: '/students' },
    { label: 'My School', icon: 'business', route: '/my-school' },
  ];

  constructor() {
    this.breakpointObserver.observe('(min-width: 960px)').subscribe(result => {
      this.isDesktop.set(result.matches);
    });
  }

  protected closeMobileSidenav(): void {
    if (!this.isDesktop()) {
      this.sidenav()?.close();
    }
  }
}
