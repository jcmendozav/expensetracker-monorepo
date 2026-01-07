import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  Auth,
  browserSessionPersistence,
  GoogleAuthProvider,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  user,
  User,
  getIdToken,
  getRedirectResult,
} from '@angular/fire/auth';
import { setPersistence } from 'firebase/auth';
import { from, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private auth: Auth = inject(Auth);
  private user$ = user(this.auth);
  user = toSignal(this.user$, { initialValue: this.auth.currentUser });

  constructor() {
    this.setSessionStoragePersistence();
  }

  private setSessionStoragePersistence(): void {
    setPersistence(this.auth, browserSessionPersistence);
  }

  login(email: string, password: string): Observable<void> {
    const promise = signInWithEmailAndPassword(this.auth, email, password).then(
      () => {
        //
      }
    );
    return from(promise);
  }

  async googleLogin(): Promise<void> {
    const provider = new GoogleAuthProvider();
    try {
      const result = await signInWithPopup(this.auth, provider);
      const user = result.user;
      if (!user) {
        throw new Error('Google-Login error');
      }
    } catch (error) {
      console.error('Google-Login error:', error);
      throw error;
    }
  }

  logout(): Observable<void> {
    const promise = signOut(this.auth).then(() => {
      sessionStorage.clear();
    });
    return from(promise);
  }

  /**
   * Retrieves the current JWT (ID Token).
   * This is used by the HttpInterceptor to talk to Spring Boot.
   */
  async getToken(): Promise<string | null> {
    const currentUser = this.auth.currentUser;
    if (!currentUser) return null;

    try {
      // Retrieves the token, refreshing it if it's near expiration
      return await getIdToken(currentUser);
    } catch (error) {
      console.error('Error getting Firebase ID Token:', error);
      return null;
    }
  }

  async loginWithGoogle() {
    const provider = new GoogleAuthProvider();
    // Switch back to Popup
    return await signInWithPopup(this.auth, provider);
  }

  getRedirectResult() {
    return getRedirectResult(this.auth);
  }
}
