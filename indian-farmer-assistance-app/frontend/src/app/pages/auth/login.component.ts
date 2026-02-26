import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h1>Indian Farmer Assistance</h1>
        <h2>{{ isAdminLogin ? 'Admin Login' : 'Farmer Login' }}</h2>
        
        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="farmerId">{{ isAdminLogin ? 'Phone Number' : 'Farmer ID' }}</label>
            <input
              id="farmerId"
              type="text"
              formControlName="farmerId"
              [placeholder]="isAdminLogin ? 'Enter your Phone Number' : 'Enter your Farmer ID'"
              class="form-control"
            />
            <div *ngIf="loginForm.get('farmerId')?.invalid && loginForm.get('farmerId')?.touched" class="error">
              {{ isAdminLogin ? 'Phone Number' : 'Farmer ID' }} is required
            </div>
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              placeholder="Enter your password"
              class="form-control"
            />
            <div *ngIf="loginForm.get('password')?.invalid && loginForm.get('password')?.touched" class="error">
              Password is required
            </div>
          </div>

          <button type="submit" [disabled]="!loginForm.valid || isLoading" class="btn-primary">
            {{ isLoading ? 'Logging in...' : 'Login' }}
          </button>
        </form>

        <p class="register-link">
          <a (click)="toggleAdminLogin()">
            {{ isAdminLogin ? 'Switch to Farmer Login' : 'Login as Admin' }}
          </a>
        </p>

        <p class="register-link" *ngIf="!isAdminLogin">
          Don't have an account? <a (click)="goToRegister()">Register here</a>
        </p>

        <div *ngIf="!isOnline" class="offline-notice">
          <p>You are offline. Attempting offline login with cached credentials.</p>
          <button type="button" (click)="loginOffline()" class="btn-secondary">
            Login Offline
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .login-card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      width: 100%;
      max-width: 400px;
    }

    h1 {
      color: #667eea;
      text-align: center;
      margin-bottom: 0.5rem;
      font-size: 1.5rem;
    }

    h2 {
      text-align: center;
      color: #333;
      margin-bottom: 1.5rem;
    }

    .form-group {
      margin-bottom: 1rem;
    }

    label {
      display: block;
      margin-bottom: 0.5rem;
      color: #333;
      font-weight: 500;
    }

    .form-control {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .form-control:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .error {
      color: #e74c3c;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }

    .btn-primary {
      width: 100%;
      padding: 0.75rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.3s;
    }

    .btn-primary:hover:not(:disabled) {
      background: #5568d3;
    }

    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .register-link {
      text-align: center;
      margin-top: 1rem;
      color: #666;
    }

    .register-link a {
      color: #667eea;
      cursor: pointer;
      text-decoration: none;
      font-weight: 600;
    }

    .offline-notice {
      margin-top: 1rem;
      padding: 1rem;
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      color: #856404;
    }

    .btn-secondary {
      width: 100%;
      padding: 0.75rem;
      background: #6c757d;
      color: white;
      border: none;
      border-radius: 4px;
      font-size: 1rem;
      cursor: pointer;
      margin-top: 0.5rem;
    }
  `]
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  isLoading = false;
  isOnline = navigator.onLine;
  isAdminLogin = false; // Toggle state

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.loginForm = this.fb.group({
      farmerId: ['', Validators.required], // We reuse this for "phone" when admin login
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    window.addEventListener('online', () => this.isOnline = true);
    window.addEventListener('offline', () => this.isOnline = false);
  }

  toggleAdminLogin(): void {
    this.isAdminLogin = !this.isAdminLogin;
    this.loginForm.reset();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.isLoading = true;
    const { farmerId, password } = this.loginForm.value;

    if (this.isAdminLogin) {
      this.authService.adminLogin({ phone: farmerId, password }).subscribe({
        next: () => {
          this.toastr.success('Admin Login successful!');
          this.router.navigate(['/admin']);
        },
        error: (error) => {
          this.isLoading = false;
          this.toastr.error('Admin Login failed. Please check your credentials.');
        }
      });
    } else {
      this.authService.login({ farmerId, password }).subscribe({
        next: () => {
          this.toastr.success('Farmer Login successful!');
          this.router.navigate(['/']);
        },
        error: (error) => {
          this.isLoading = false;
          this.toastr.error('Login failed. Please check your credentials.');
        }
      });
    }
  }

  loginOffline(): void {
    const { farmerId, password } = this.loginForm.value;
    if (this.authService.loginOffline(farmerId, password)) {
      this.toastr.success('Offline login successful!');
      this.router.navigate(['/']);
    } else {
      this.toastr.error('Offline login failed. No cached credentials found.');
    }
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }
}

