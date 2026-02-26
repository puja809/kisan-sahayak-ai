import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, LoginRequest, LoginResponse } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('login', () => {
    it('should authenticate user with valid credentials', (done) => {
      const loginRequest: LoginRequest = {
        farmerId: 'FARMER123',
        password: 'password123'
      };

      const mockResponse: LoginResponse = {
        token: 'mock-jwt-token',
        farmerId: 'FARMER123',
        name: 'John Farmer',
        preferredLanguage: 'en'
      };

      service.login(loginRequest).subscribe({
        next: (response) => {
          expect(response.token).toBe('mock-jwt-token');
          expect(localStorage.getItem('auth_token')).toBe('mock-jwt-token');
          expect(service.isAuthenticated()).toBe(true);
          done();
        }
      });

      const req = httpMock.expectOne('/api/v1/users/login');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should handle login failure', (done) => {
      const loginRequest: LoginRequest = {
        farmerId: 'INVALID',
        password: 'wrong'
      };

      service.login(loginRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(service.isAuthenticated()).toBe(false);
          done();
        }
      });

      const req = httpMock.expectOne('/api/v1/users/login');
      req.error(new ErrorEvent('Unauthorized'));
    });
  });

  describe('register', () => {
    it('should register new user', (done) => {
      const registerData = {
        name: 'New Farmer',
        farmerId: 'NEWFARM123',
        phone: '9876543210',
        email: 'farmer@example.com',
        preferredLanguage: 'en',
        state: 'Karnataka',
        district: 'Bangalore',
        password: 'password123'
      };

      const mockResponse: LoginResponse = {
        token: 'mock-jwt-token',
        farmerId: 'NEWFARM123',
        name: 'New Farmer',
        preferredLanguage: 'en'
      };

      service.register(registerData).subscribe({
        next: (response) => {
          expect(response.token).toBe('mock-jwt-token');
          expect(service.isAuthenticated()).toBe(true);
          done();
        }
      });

      const req = httpMock.expectOne('/api/v1/users/register');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('logout', () => {
    it('should clear authentication data', () => {
      localStorage.setItem('auth_token', 'mock-token');
      localStorage.setItem('cached_user', JSON.stringify({ farmerId: 'FARMER123', name: 'John' }));

      service.logout();

      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('cached_user')).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('offline authentication', () => {
    it('should allow offline login with cached credentials', () => {
      const cachedUser = {
        farmerId: 'FARMER123',
        name: 'John Farmer',
        phone: '9876543210',
        preferredLanguage: 'en',
        state: 'Karnataka',
        district: 'Bangalore'
      };

      localStorage.setItem('cached_user', JSON.stringify(cachedUser));

      const result = service.loginOffline('FARMER123', 'password123');

      expect(result).toBe(true);
      expect(service.getCurrentUser()).toEqual(cachedUser);
    });

    it('should reject offline login with invalid credentials', () => {
      const cachedUser = {
        farmerId: 'FARMER123',
        name: 'John Farmer',
        phone: '9876543210',
        preferredLanguage: 'en',
        state: 'Karnataka',
        district: 'Bangalore'
      };

      localStorage.setItem('cached_user', JSON.stringify(cachedUser));

      const result = service.loginOffline('INVALID', 'password123');

      expect(result).toBe(false);
    });
  });

  describe('token management', () => {
    it('should retrieve stored token', () => {
      localStorage.setItem('auth_token', 'mock-token');

      const token = service.getToken();

      expect(token).toBe('mock-token');
    });

    it('should check authentication status', () => {
      expect(service.isAuthenticated()).toBe(false);

      localStorage.setItem('auth_token', 'mock-token');

      expect(service.isAuthenticated()).toBe(true);
    });
  });

  describe('current user', () => {
    it('should retrieve current user from cache', () => {
      const cachedUser = {
        farmerId: 'FARMER123',
        name: 'John Farmer',
        phone: '9876543210',
        email: 'farmer@example.com',
        preferredLanguage: 'en',
        state: 'Karnataka',
        district: 'Bangalore'
      };

      localStorage.setItem('cached_user', JSON.stringify(cachedUser));

      const user = service.getCurrentUser();

      expect(user).toEqual(cachedUser);
    });

    it('should return null when no user is cached', () => {
      const user = service.getCurrentUser();

      expect(user).toBeNull();
    });
  });
});
