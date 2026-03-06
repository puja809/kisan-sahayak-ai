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
  styleUrls: ['./login.component.css'],
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
      email: ['', [Validators.required, Validators.email]],
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
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;
    const { email, password } = this.loginForm.value;

    const credentials = { email, password };

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
    const { email, password } = this.loginForm.value;
    if (this.authService.loginOffline(email, password)) {
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
