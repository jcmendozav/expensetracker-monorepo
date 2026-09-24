import { ChangeDetectionStrategy, Component, inject, HostListener, signal } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

// md breakpoint = 960px — matches Angular CDK Breakpoints.Medium
const MD_BREAKPOINT = 960;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    CommonModule,
    NgOptimizedImage,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  title = 'expensetracker';
  authService = inject(AuthService);
  private router = inject(Router);

  isMobile = window.innerWidth < MD_BREAKPOINT;

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth < MD_BREAKPOINT;
  }

  async logout() {
    try {
      await firstValueFrom(this.authService.logout());
      this.router.navigate(['/login']);
    } catch (err) {
      console.error('Logout failed', err);
    }
  }
}