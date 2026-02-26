import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FertilizerSupplierService } from '../../services/fertilizer-supplier.service';

interface FertilizerSupplier {
  state: string;
  district: string;
  documentId: string;
  slNo: number;
  noOfWholesalers: number;
  noOfRetailers: number;
  fertilizerType: string;
  supplierName: string;
  contactInfo: string;
  totalSuppliers?: number;
}

interface FilterOptions {
  states: string[];
  districts: string[];
  fertilizerTypes: string[];
}

@Component({
  selector: 'app-fertilizer-suppliers',
  templateUrl: './fertilizer-suppliers.component.html',
  styleUrls: ['./fertilizer-suppliers.component.css']
})
export class FertilizerSuppliersComponent implements OnInit {
  
  suppliers: FertilizerSupplier[] = [];
  filteredSuppliers: FertilizerSupplier[] = [];
  filterForm: FormGroup;
  loading = false;
  error: string | null = null;
  sortBy: 'wholesalers' | 'retailers' | 'total' = 'total';
  
  filterOptions: FilterOptions = {
    states: [],
    districts: [],
    fertilizerTypes: []
  };

  // Pagination
  pageSize = 10;
  currentPage = 1;
  totalRecords = 0;

  constructor(
    private fb: FormBuilder,
    private fertilizerSupplierService: FertilizerSupplierService
  ) {
    this.filterForm = this.fb.group({
      state: ['', Validators.required],
      district: ['', Validators.required],
      fertilizerType: [''],
      sortBy: ['total']
    });
  }

  ngOnInit(): void {
    this.loadFilterOptions();
    this.setupFormValueChanges();
  }

  /**
   * Load available filter options.
   */
  private loadFilterOptions(): void {
    // Load states from service
    this.fertilizerSupplierService.getStates().subscribe(
      (states) => {
        this.filterOptions.states = states;
      },
      (error) => {
        console.error('Error loading states:', error);
      }
    );

    // Load fertilizer types
    this.filterOptions.fertilizerTypes = [
      'Urea',
      'DAP',
      'MOP',
      'NPK',
      'Ammonium Nitrate',
      'Calcium Ammonium Nitrate',
      'Organic Fertilizer',
      'Biofertilizer'
    ];
  }

  /**
   * Setup form value change listeners.
   */
  private setupFormValueChanges(): void {
    this.filterForm.get('state')?.valueChanges.subscribe((state) => {
      if (state) {
        this.loadDistricts(state);
        this.filterForm.get('district')?.reset();
      }
    });

    this.filterForm.get('sortBy')?.valueChanges.subscribe((sortBy) => {
      this.sortBy = sortBy;
      this.applySort();
    });
  }

  /**
   * Load districts for selected state.
   */
  private loadDistricts(state: string): void {
    this.fertilizerSupplierService.getDistricts(state).subscribe(
      (districts) => {
        this.filterOptions.districts = districts;
      },
      (error) => {
        console.error('Error loading districts:', error);
      }
    );
  }

  /**
   * Search for fertilizer suppliers.
   */
  searchSuppliers(): void {
    if (!this.filterForm.valid) {
      this.error = 'Please select state and district';
      return;
    }

    this.loading = true;
    this.error = null;
    this.currentPage = 1;

    const { state, district, fertilizerType } = this.filterForm.value;

    if (fertilizerType) {
      this.fertilizerSupplierService.getSuppliersByType(state, district, fertilizerType)
        .subscribe(
          (suppliers) => {
            this.suppliers = suppliers;
            this.filteredSuppliers = suppliers;
            this.totalRecords = suppliers.length;
            this.applySort();
            this.loading = false;
          },
          (error) => {
            this.error = 'Error loading suppliers: ' + error.message;
            this.loading = false;
          }
        );
    } else {
      this.fertilizerSupplierService.getSuppliersByLocation(state, district)
        .subscribe(
          (suppliers) => {
            this.suppliers = suppliers;
            this.filteredSuppliers = suppliers;
            this.totalRecords = suppliers.length;
            this.applySort();
            this.loading = false;
          },
          (error) => {
            this.error = 'Error loading suppliers: ' + error.message;
            this.loading = false;
          }
        );
    }
  }

  /**
   * Apply sorting to suppliers.
   */
  private applySort(): void {
    const sorted = [...this.filteredSuppliers];
    
    switch (this.sortBy) {
      case 'wholesalers':
        sorted.sort((a, b) => (b.noOfWholesalers || 0) - (a.noOfWholesalers || 0));
        break;
      case 'retailers':
        sorted.sort((a, b) => (b.noOfRetailers || 0) - (a.noOfRetailers || 0));
        break;
      case 'total':
      default:
        sorted.sort((a, b) => {
          const totalA = (a.noOfWholesalers || 0) + (a.noOfRetailers || 0);
          const totalB = (b.noOfWholesalers || 0) + (b.noOfRetailers || 0);
          return totalB - totalA;
        });
    }
    
    this.filteredSuppliers = sorted;
  }

  /**
   * Get paginated suppliers.
   */
  getPaginatedSuppliers(): FertilizerSupplier[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    return this.filteredSuppliers.slice(startIndex, endIndex);
  }

  /**
   * Get total pages.
   */
  getTotalPages(): number {
    return Math.ceil(this.totalRecords / this.pageSize);
  }

  /**
   * Go to next page.
   */
  nextPage(): void {
    if (this.currentPage < this.getTotalPages()) {
      this.currentPage++;
    }
  }

  /**
   * Go to previous page.
   */
  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  /**
   * Call supplier.
   */
  callSupplier(supplier: FertilizerSupplier): void {
    if (supplier.contactInfo) {
      window.location.href = `tel:${supplier.contactInfo}`;
    }
  }

  /**
   * Get total supplier count.
   */
  getTotalSuppliers(supplier: FertilizerSupplier): number {
    return (supplier.noOfWholesalers || 0) + (supplier.noOfRetailers || 0);
  }

  /**
   * Get supplier availability status.
   */
  getAvailabilityStatus(supplier: FertilizerSupplier): string {
    const total = this.getTotalSuppliers(supplier);
    if (total > 20) {
      return 'Widely Available';
    } else if (total > 10) {
      return 'Moderately Available';
    } else {
      return 'Limited Availability';
    }
  }

  /**
   * Get availability status color.
   */
  getStatusColor(supplier: FertilizerSupplier): string {
    const total = this.getTotalSuppliers(supplier);
    if (total > 20) {
      return 'success';
    } else if (total > 10) {
      return 'warning';
    } else {
      return 'danger';
    }
  }
}
