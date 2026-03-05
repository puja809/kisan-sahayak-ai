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
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],})
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

