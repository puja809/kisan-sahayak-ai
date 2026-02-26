import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="register-container">
      <div class="register-card">
        <h1>Indian Farmer Assistance</h1>
        <h2>Register</h2>
        
        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="name">Full Name</label>
            <input
              id="name"
              type="text"
              formControlName="name"
              placeholder="Enter your full name"
              class="form-control"
            />
            <div *ngIf="registerForm.get('name')?.invalid && registerForm.get('name')?.touched" class="error">
              Name is required
            </div>
          </div>

          <div class="form-group">
            <label for="farmerId">Farmer ID</label>
            <input
              id="farmerId"
              type="text"
              formControlName="farmerId"
              placeholder="Enter your Farmer ID"
              class="form-control"
            />
            <div *ngIf="registerForm.get('farmerId')?.invalid && registerForm.get('farmerId')?.touched" class="error">
              Farmer ID is required
            </div>
          </div>

          <div class="form-group">
            <label for="phone">Phone Number</label>
            <input
              id="phone"
              type="tel"
              formControlName="phone"
              placeholder="Enter your phone number"
              class="form-control"
            />
            <div *ngIf="registerForm.get('phone')?.invalid && registerForm.get('phone')?.touched" class="error">
              Valid phone number is required
            </div>
          </div>

          <div class="form-group">
            <label for="email">Email (Optional)</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              placeholder="Enter your email"
              class="form-control"
            />
          </div>

          <div class="form-group">
            <label for="state">State</label>
            <select formControlName="state" class="form-control">
              <option value="">Select State</option>
              <option value="Karnataka">Karnataka</option>
              <option value="Maharashtra">Maharashtra</option>
              <option value="Telangana">Telangana</option>
              <option value="Andhra Pradesh">Andhra Pradesh</option>
              <option value="Haryana">Haryana</option>
              <option value="Uttar Pradesh">Uttar Pradesh</option>
              <option value="Punjab">Punjab</option>
            </select>
            <div *ngIf="registerForm.get('state')?.invalid && registerForm.get('state')?.touched" class="error">
              State is required
            </div>
          </div>

          <div class="form-group">
            <label for="district">District</label>
            <input
              id="district"
              type="text"
              formControlName="district"
              placeholder="Enter your district"
              class="form-control"
            />
            <div *ngIf="registerForm.get('district')?.invalid && registerForm.get('district')?.touched" class="error">
              District is required
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
            <div *ngIf="registerForm.get('password')?.invalid && registerForm.get('password')?.touched" class="error">
              Password must be at least 6 characters
            </div>
          </div>

          <button type="submit" [disabled]="!registerForm.valid || isLoading" class="btn-primary">
            {{ isLoading ? 'Registering...' : 'Register' }}
          </button>
        </form>

        <p class="login-link">
          Already have an account? <a (click)="goToLogin()">Login here</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 1rem;
    }

    .register-card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      width: 100%;
      max-width: 500px;
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
      box-sizing: border-box;
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

    .login-link {
      text-align: center;
      margin-top: 1rem;
      color: #666;
    }

    .login-link a {
      color: #667eea;
      cursor: pointer;
      text-decoration: none;
      font-weight: 600;
    }
  `]
})
export class RegisterComponent {
  registerForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.registerForm = this.fb.group({
      name: ['', Validators.required],
      farmerId: ['', Validators.required],
      phone: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      email: ['', Validators.email],
      state: ['', Validators.required],
      district: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;

    this.isLoading = true;
    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.toastr.success('Registration successful!');
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isLoading = false;
        this.toastr.error('Registration failed. Please try again.');
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
