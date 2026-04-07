import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../shared/auth/auth.service';

interface NavChild {
  label: string;
  route: string;
}

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  children?: NavChild[];
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
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected auth = inject(AuthService);
  protected user = this.auth.user;

  private router = inject(Router);
  private breakpointObserver = inject(BreakpointObserver);
  private destroyRef = inject(DestroyRef);
  private sidenav = viewChild<MatSidenav>('sidenav');

  protected isDesktop = signal(true);
  protected mySchoolExpanded = signal(true);

  protected navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/app/dashboard' },
    {
      label: 'My School', icon: 'business', children: [
        { label: 'Profile', route: '/app/my-school' },
        { label: 'Subscriptions', route: '/app/subscriptions' },
        { label: 'Students', route: '/app/students' },
      ],
    },
    { label: 'Courses', icon: 'school', route: '/app/courses' },
    { label: 'Payments', icon: 'payments', route: '/app/payments' },
  ];

  constructor() {
    this.breakpointObserver.observe('(min-width: 960px)').pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(result => {
      this.isDesktop.set(result.matches);
    });
  }

  protected toggleSection(): void {
    this.mySchoolExpanded.update(v => !v);
  }

  protected isChildRouteActive(item: NavItem): boolean {
    if (!item.children) return false;
    return item.children.some(child => this.router.isActive(child.route, {
      paths: 'subset',
      queryParams: 'subset',
      fragment: 'ignored',
      matrixParams: 'ignored',
    }));
  }

  protected closeMobileSidenav(): void {
    if (!this.isDesktop()) {
      this.sidenav()?.close();
    }
  }
}
