import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { StateDistrictService, State, District } from '../../services/state-district.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
})
export class RegisterComponent {
  registerForm: FormGroup;
  isLoading = false;
  states: State[] = [];
  districts: District[] = [];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private stateDistrictService: StateDistrictService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      phone: ['', [Validators.pattern(/^\d{10}$/)]],
      state: [''],
      district: [''],
      village: [''],
      pinCode: ['', [Validators.pattern(/^[1-9]\d{5}$/)]]
    });
  }

  ngOnInit(): void {
    this.loadStates();
  }

  loadStates(): void {
    this.stateDistrictService.getAllStates().subscribe({
      next: (states) => this.states = states,
      error: (err) => console.error('Failed to load states', err)
    });
  }

  onStateChange(): void {
    const stateName = this.registerForm.get('state')?.value;
    if (stateName) {
      this.stateDistrictService.getDistrictsByState(stateName).subscribe({
        next: (districts) => {
          this.districts = districts;
          this.registerForm.get('district')?.setValue('');
        },
        error: (err) => console.error('Failed to load districts', err)
      });
    } else {
      this.districts = [];
      this.registerForm.get('district')?.setValue('');
    }
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.toastr.error('Please fill in all required fields correctly');
      return;
    }

    this.isLoading = true;
    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.toastr.success('Registration successful!');
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isLoading = false;
        const errorMsg = error?.error?.message || 'Registration failed. Please try again.';
        this.toastr.error(errorMsg);
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
