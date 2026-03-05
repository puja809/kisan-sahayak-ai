import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export interface UserLoginRequest {
  email: string;
  password: string;
}

export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  farmerId: string;
  name: string;
  phone?: string;
  email: string;
  preferredLanguage: string;
  state?: string;
  district?: string;
  role: string;
  isActive: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
  expiresIn: number;
}

export interface User {
  farmerId: string;
  name: string;
  phone?: string;
  email: string;
  preferredLanguage: string;
  state?: string;
  district?: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/v1/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(this.getCachedUser());
  public currentUser$ = this.currentUserSubject.asObservable();
  private tokenKey = 'auth_token';
  private userKey = 'cached_user';

  constructor(private http: HttpClient) {
    this.checkTokenValidity();
  }

  /**
   * User login with email and password
   */
  userLogin(request: UserLoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        console.error('User login failed:', error);
        throw error;
      })
    );
  }

  /**
   * Admin login with email and password
   */
  adminLogin(request: AdminLoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/admin-login`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        console.error('Admin login failed:', error);
        throw error;
      })
    );
  }

  /**
   * General login (can be redirected to userLogin or adminLogin based on email)
   * The backend currently has separate endpoints, so we'll use user-login for now
   */
  login(email: string, password: string): Observable<LoginResponse> {
    return this.userLogin({ email, password });
  }

  register(userData: any): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/register`, userData).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        console.error('Registration failed:', error);
        throw error;
      })
    );
  }

  private handleAuthResponse(response: LoginResponse): void {
    if (response.accessToken) {
      localStorage.setItem(this.tokenKey, response.accessToken);
      const user: User = {
        farmerId: response.user.farmerId,
        name: response.user.name,
        phone: response.user.phone,
        email: response.user.email,
        preferredLanguage: response.user.preferredLanguage,
        state: response.user.state,
        district: response.user.district,
        role: response.user.role
      };
      localStorage.setItem(this.userKey, JSON.stringify(user));
      this.currentUserSubject.next(user);
      console.log('User session saved to localStorage:', user.email);
    }
  }

  logout(): void {
    console.warn('AuthService: logout() called. Clearing session.');
    console.trace(); // Log stack trace to see what triggered logout
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  private getCachedUser(): User | null {
    const cached = localStorage.getItem(this.userKey);
    return cached ? JSON.parse(cached) : null;
  }

  private checkTokenValidity(): void {
    const token = this.getToken();
    const user = this.getCachedUser();

    if (token && user) {
      console.log('AuthService: Token found for user:', user.email);
    } else if (token || user) {
      console.warn('AuthService: Inconsistent session state. Token:', !!token, 'User:', !!user);
      // If we have a token but no user, or vice versa, it might cause issues
    } else {
      console.log('AuthService: No active session found.');
    }
  }

  // Offline authentication can be implemented here if needed
  loginOffline(email: string, password: string): boolean {
    const cachedUser = this.getCachedUser();
    if (cachedUser && cachedUser.email === email) {
      this.currentUserSubject.next(cachedUser);
      return true;
    }
    return false;
  }
}
