import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = signal('');
  password = signal('');
  errorMessage = signal('');
  isLoading = signal(true);

  constructor() {
    effect(() => {
      if (this.authService.user()) {
        this.router.navigate(['/dashboard']);
      } else {
        this.isLoading.set(false);
      }
    });
  }

  ngOnInit(): void {
    this.authService.getRedirectResult().catch((error) => {
      this.errorMessage.set(error.message);
    });
  }

  async onLogin() {
    this.isLoading.set(true);
    this.errorMessage.set('');
    try {
      await firstValueFrom(this.authService.login(this.email(), this.password()));
    } catch (error: any) {
      this.errorMessage.set(error.message);
    } finally {
      this.isLoading.set(false);
    }
  }

  async onGoogleLogin() {
    this.isLoading.set(true);
    this.errorMessage.set('');
    try {
      await this.authService.loginWithGoogle();
    } catch (error: any) {
      this.errorMessage.set('Google Sign-In failed. Please try again.');
    } finally {
      this.isLoading.set(false);
    }
  }
}