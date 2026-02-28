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
            <label>Login with:</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" formControlName="loginType" value="email" />
                Email
              </label>
              <label class="radio-label">
                <input type="radio" formControlName="loginType" value="phone" />
                Phone
              </label>
            </div>
          </div>

          <div class="form-group" *ngIf="loginForm.get('loginType')?.value === 'email'">
            <label for="email">Email Address</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              placeholder="Enter your email"
              class="form-control"
            />
            <div *ngIf="loginForm.hasError('emailRequired')" class="error">
              Email is required
            </div>
            <div *ngIf="loginForm.hasError('invalidEmail')" class="error">
              Please enter a valid email
            </div>
          </div>

          <div class="form-group" *ngIf="loginForm.get('loginType')?.value === 'phone'">
            <label for="phone">Phone Number</label>
            <input
              id="phone"
              type="tel"
              formControlName="phone"
              placeholder="Enter your phone number"
              class="form-control"
            />
            <div *ngIf="loginForm.hasError('phoneRequired')" class="error">
              Phone number is required
            </div>
            <div *ngIf="loginForm.hasError('invalidPhone')" class="error">
              Please enter a valid 10-digit phone number
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
      background: linear-gradient(135deg, #1B5E20 0%, #2E7D32 50%, #388E3C 100%);
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
      color: #1B5E20;
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

    .radio-group {
      display: flex;
      gap: 1rem;
      margin-bottom: 0.5rem;
    }

    .radio-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: normal;
      cursor: pointer;
    }

    .radio-label input[type="radio"] {
      cursor: pointer;
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
      border-color: #2E7D32;
      box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.1);
    }

    .error {
      color: #e74c3c;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }

    .btn-primary {
      width: 100%;
      padding: 0.75rem;
      background: #2E7D32;
      color: white;
      border: none;
      border-radius: 4px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.3s;
    }

    .btn-primary:hover:not(:disabled) {
      background: #1B5E20;
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
      color: #2E7D32;
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
  isAdminLogin = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.loginForm = this.fb.group({
      loginType: ['email', Validators.required],
      email: [''],
      phone: [''],
      password: ['', Validators.required]
    }, { validators: this.identifierValidator });
  }

  /**
   * Custom validator to ensure either email or phone is provided based on loginType
   */
  identifierValidator(group: FormGroup): { [key: string]: any } | null {
    const loginType = group.get('loginType')?.value;
    const email = group.get('email')?.value;
    const phone = group.get('phone')?.value;

    if (loginType === 'email') {
      if (!email || email.trim() === '') {
        return { 'emailRequired': true };
      }
      // Validate email format
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(email)) {
        return { 'invalidEmail': true };
      }
    } else if (loginType === 'phone') {
      if (!phone || phone.trim() === '') {
        return { 'phoneRequired': true };
      }
      // Validate phone format
      const phoneRegex = /^[6-9]\d{9}$/;
      if (!phoneRegex.test(phone)) {
        return { 'invalidPhone': true };
      }
    }

    return null;
  }

  ngOnInit(): void {
    window.addEventListener('online', () => this.isOnline = true);
    window.addEventListener('offline', () => this.isOnline = false);
  }

  toggleAdminLogin(): void {
    this.isAdminLogin = !this.isAdminLogin;
    this.loginForm.reset({ loginType: 'email' });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.toastr.error('Please fill in all required fields correctly');
      return;
    }

    this.isLoading = true;
    const { loginType, email, phone, password } = this.loginForm.value;

    const credentials = {
      email: loginType === 'email' ? email : undefined,
      phone: loginType === 'phone' ? phone : undefined,
      password
    };

    if (this.isAdminLogin) {
      this.authService.adminLogin(credentials).subscribe({
        next: () => {
          this.toastr.success('Admin Login successful!');
          this.router.navigate(['/admin']);
        },
        error: (error) => {
          this.isLoading = false;
          const errorMsg = error?.error?.message || 'Admin Login failed. Please check your credentials.';
          this.toastr.error(errorMsg);
        }
      });
    } else {
      this.authService.userLogin(credentials).subscribe({
        next: () => {
          this.toastr.success('Farmer Login successful!');
          this.router.navigate(['/']);
        },
        error: (error) => {
          this.isLoading = false;
          const errorMsg = error?.error?.message || 'Login failed. Please check your credentials.';
          this.toastr.error(errorMsg);
        }
      });
    }
  }

  loginOffline(): void {
    const { email, phone, password } = this.loginForm.value;
    const identifier = email || phone;
    if (this.authService.loginOffline(identifier, password)) {
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

