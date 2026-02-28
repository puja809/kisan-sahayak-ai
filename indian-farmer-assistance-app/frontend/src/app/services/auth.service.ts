import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export interface LoginRequest {
  farmerId: string;
  password: string;
}

export interface UserLoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export interface AdminLoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export interface UserResponse {
  farmerId: string;
  name: string;
  phone: string;
  email?: string;
  preferredLanguage: string;
  state: string;
  district: string;
  role?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
}

export interface User {
  farmerId: string;
  name: string;
  phone: string;
  email?: string;
  preferredLanguage: string;
  state: string;
  district: string;
  role?: string;
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
   * User login with email or phone and password
   */
  userLogin(request: UserLoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/user-login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.accessToken);
        const user: User = {
          farmerId: response.user.farmerId,
          name: response.user.name,
          phone: response.user.phone || '',
          email: response.user.email || '',
          preferredLanguage: response.user.preferredLanguage || 'en',
          state: response.user.state || '',
          district: response.user.district || '',
          role: response.user.role || 'FARMER'
        };
        localStorage.setItem(this.userKey, JSON.stringify(user));
        this.currentUserSubject.next(user);
      }),
      catchError(error => {
        console.error('User login failed:', error);
        throw error;
      })
    );
  }

  /**
   * Admin login with email or phone and password
   */
  adminLogin(request: AdminLoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/admin-login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.accessToken);
        const user: User = {
          farmerId: response.user.farmerId,
          name: response.user.name,
          phone: response.user.phone || '',
          email: response.user.email || '',
          preferredLanguage: response.user.preferredLanguage || 'en',
          state: response.user.state || '',
          district: response.user.district || '',
          role: response.user.role || 'ADMIN'
        };
        localStorage.setItem(this.userKey, JSON.stringify(user));
        this.currentUserSubject.next(user);
      }),
      catchError(error => {
        console.error('Admin login failed:', error);
        throw error;
      })
    );
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.accessToken);
        const user: User = {
          farmerId: response.user.farmerId,
          name: response.user.name,
          phone: response.user.phone || '',
          preferredLanguage: response.user.preferredLanguage || 'en',
          state: response.user.state || '',
          district: response.user.district || '',
          role: response.user.role || 'FARMER'
        };
        localStorage.setItem(this.userKey, JSON.stringify(user));
        this.currentUserSubject.next(user);
      }),
      catchError(error => {
        console.error('Login failed:', error);
        throw error;
      })
    );
  }

  register(user: User & { password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/register`, user).pipe(
      tap(response => {
        if (response.accessToken) {
          localStorage.setItem(this.tokenKey, response.accessToken);
          const cachedUser: User = {
            farmerId: response.user?.farmerId || user.farmerId,
            name: response.user?.name || user.name,
            phone: response.user?.phone || user.phone,
            email: response.user?.email || user.email,
            preferredLanguage: response.user?.preferredLanguage || user.preferredLanguage,
            state: response.user?.state || user.state,
            district: response.user?.district || user.district,
            role: response.user?.role || 'FARMER'
          };
          localStorage.setItem(this.userKey, JSON.stringify(cachedUser));
          this.currentUserSubject.next(cachedUser);
        }
      }),
      catchError(error => {
        console.error('Registration failed:', error);
        throw error;
      })
    );
  }

  logout(): void {
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
    if (token) {
      // Verify token with backend
      this.http.get<{ valid: boolean }>(`${this.apiUrl}/verify-token`).pipe(
        catchError(() => {
          this.logout();
          return of({ valid: false });
        })
      ).subscribe();
    }
  }

  // Offline authentication with cached credentials
  loginOffline(farmerId: string, password: string): boolean {
    const cachedUser = this.getCachedUser();
    if (cachedUser && cachedUser.farmerId === farmerId) {
      // In production, validate password hash
      this.currentUserSubject.next(cachedUser);
      return true;
    }
    return false;
  }
}
