import { Component, inject, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms'; // Needed for [(ngModel)]
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { getRedirectResult } from '@angular/fire/auth'; // <--- Import this
import { Auth } from '@angular/fire/auth'; // <--- Import this

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private auth = inject(Auth); // <--- Inject Auth directly

  email = '';
  password = '';
  errorMessage = '';
  isLoading = true; // Start true to prevent flickering

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      if (user) {
        this.router.navigate(['/dashboard']);
      }
    });
  }

  async onLogin() {
    // 1. Check if we are returning from a Google Redirect
    getRedirectResult(this.auth).then((result) => {
      if (result) {
        console.log('Redirect success! User:', result.user);
        this.router.navigate(['/dashboard']);
      }
      this.isLoading = false;
    }).catch((error) => {
      console.error('Redirect Error:', error);
      this.errorMessage = error.message;
      this.isLoading = false;
    });

    // 2. Also listen for normal auth state changes (persistence)
    this.authService.user$.subscribe(user => {
      if (user) {
        console.log('Auth State: User found, going to dashboard');
        this.router.navigate(['/dashboard']);
      } else {
        this.isLoading = false;
      }
    });
  }

  // Optional: Add Google Login here if you implemented it in AuthService
  async onGoogleLogin() {
    this.isLoading = true;
    try {
      // 1. Open the Popup
      await this.authService.loginWithGoogle();

      // 2. If successful, the user$ subscription in ngOnInit handles the redirect!
      // But we can also force it here:
      this.router.navigate(['/dashboard']);

    } catch (error: any) {
      console.error('Popup Error:', error);
      this.errorMessage = 'Google Sign-In failed. Please try again.';
      this.isLoading = false;
    }
  }

}