import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { YieldCalculatorService, YieldCalculationResponse } from '../../services/yield-calculator.service';

@Component({
  selector: 'app-yield-calculator',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './yield-calculator.component.html',
  styleUrls: ['./yield-calculator.component.css']
})
export class YieldCalculatorComponent implements OnInit {

  yieldForm!: FormGroup;
  loading = false;
  loadingCommodities = true;
  result: YieldCalculationResponse | null = null;
  error: string | null = null;
  submitted = false;
  commodities: string[] = [];

  constructor(
    private fb: FormBuilder,
    private yieldCalculatorService: YieldCalculatorService
  ) { }

  ngOnInit(): void {
    this.initializeForm();
    this.loadCommodities();
  }

  initializeForm(): void {
    this.yieldForm = this.fb.group({
      commodity: ['', [Validators.required]],
      farmSizeHectares: ['', [Validators.required, Validators.min(0.1), Validators.max(1000)]],
      investmentAmount: ['', [Validators.required, Validators.min(1000), Validators.max(10000000)]]
    });
  }

  /**
   * Load commodities from mandi-service
   */
  loadCommodities(): void {
    this.loadingCommodities = true;
    this.yieldCalculatorService.getCommodities().subscribe({
      next: (commodities: string[]) => {
        this.commodities = commodities.sort();
        this.loadingCommodities = false;
      },
      error: (error) => {
        console.error('Error loading commodities:', error);
        this.loadingCommodities = false;
        this.error = 'Failed to load commodities. Please refresh the page.';
      }
    });
  }

  get f() {
    return this.yieldForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = null;
    this.result = null;

    if (this.yieldForm.invalid) {
      return;
    }

    this.loading = true;
    const request = this.yieldForm.value;

    this.yieldCalculatorService.calculateYield(request).subscribe({
      next: (response: YieldCalculationResponse) => {
        this.result = response;
        this.loading = false;
      },
      error: (error) => {
        this.error = error.error?.message || 'Error calculating yield. Please try again.';
        this.loading = false;
      }
    });
  }

  resetForm(): void {
    this.yieldForm.reset();
    this.result = null;
    this.error = null;
    this.submitted = false;
  }

  /**
   * Format number with 2 decimal places
   */
  formatNumber(value: number | undefined): string {
    if (!value) return '0.00';
    return value.toFixed(2);
  }

  /**
   * Get profit margin color based on value
   */
  getProfitMarginColor(margin: number | undefined): string {
    if (!margin) return 'text-gray-600';
    if (margin > 50) return 'text-green-600';
    if (margin > 0) return 'text-blue-600';
    return 'text-red-600';
  }
}
