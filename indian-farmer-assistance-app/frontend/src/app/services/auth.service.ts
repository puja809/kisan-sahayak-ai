import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export interface LoginRequest {
  farmerId: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  farmerId: string;
  name: string;
  preferredLanguage: string;
}

export interface User {
  farmerId: string;
  name: string;
  phone: string;
  email?: string;
  preferredLanguage: string;
  state: string;
  district: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/v1/users';
  private currentUserSubject = new BehaviorSubject<User | null>(this.getCachedUser());
  public currentUser$ = this.currentUserSubject.asObservable();
  private tokenKey = 'auth_token';
  private userKey = 'cached_user';

  constructor(private http: HttpClient) {
    this.checkTokenValidity();
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.token);
        const user: User = {
          farmerId: response.farmerId,
          name: response.name,
          phone: '',
          preferredLanguage: response.preferredLanguage,
          state: '',
          district: ''
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
        localStorage.setItem(this.tokenKey, response.token);
        const cachedUser: User = {
          farmerId: response.farmerId,
          name: response.name,
          phone: user.phone,
          email: user.email,
          preferredLanguage: response.preferredLanguage,
          state: user.state,
          district: user.district
        };
        localStorage.setItem(this.userKey, JSON.stringify(cachedUser));
        this.currentUserSubject.next(cachedUser);
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
