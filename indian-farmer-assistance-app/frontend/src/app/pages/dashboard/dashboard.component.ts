import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, User } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { ProfileService, CropResponse, CropRequest, Season, CropStatus } from '../../services/profile.service';
import { MandiService } from '../../services/mandi.service';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  crops: CropResponse[] = [];

  // Modal state
  showAddCropModal = false;
  isSubmitting = false;

  // Crop Form
  editingCropId: number | null = null;
  newCrop: CropRequest = this.resetCropForm();

  // Searchable Dropdowns
  allCrops: string[] = [];
  filteredCrops: string[] = [];
  allVarieties: string[] = [];
  filteredVarieties: string[] = [];
  showCropDropdown = false;
  showVarietyDropdown = false;

  cropsSearchSubject = new Subject<string>();
  varietiesSearchSubject = new Subject<string>();

  seasons = Object.values(Season);

  constructor(
    private authService: AuthService,
    private profileService: ProfileService,
    private mandiService: MandiService
  ) {
    // Setup search debouncing
    this.cropsSearchSubject.pipe(
      debounceTime(200),
      distinctUntilChanged()
    ).subscribe(term => {
      this.filterCrops(term);
    });

    this.varietiesSearchSubject.pipe(
      debounceTime(200),
      distinctUntilChanged()
    ).subscribe(term => {
      this.filterVarieties(term);
    });
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDashboardData();
    this.fetchMandiCrops();
  }

  private loadDashboardData(): void {
    if (!this.currentUser) return;

    // Load crops directly from profile
    this.profileService.getCrops().subscribe({
      next: (data) => this.crops = data,
      error: (error) => console.error('Failed to load crops:', error)
    });
  }

  private resetCropForm(): CropRequest {
    return {
      cropName: '',
      cropVariety: '',
      sowingDate: new Date().toISOString().split('T')[0],
      areaAcres: 1,
      season: Season.KHARIF,
      status: CropStatus.SOWN,
      seedCost: undefined,
      fertilizerCost: undefined,
      pesticideCost: undefined,
      laborCost: undefined,
      otherCost: undefined,
      totalYieldQuintals: undefined,
      qualityGrade: undefined,
      sellingPricePerQuintal: undefined,
      mandiName: undefined,
      totalRevenue: undefined,
      actualHarvestDate: undefined,
      notes: undefined
    };
  }

  // Mandi Integration Methods
  private fetchMandiCrops(): void {
    this.mandiService.getFilterCommodities().subscribe({
      next: (crops) => {
        this.allCrops = crops;
        this.filteredCrops = [...crops];
      },
      error: (err) => console.error('Failed to fetch crops from Mandi:', err)
    });
  }

  onCropSearch(term: string | undefined): void {
    const searchTerm = term || '';
    this.showCropDropdown = true;
    this.cropsSearchSubject.next(searchTerm);
    if (!searchTerm) {
      this.allVarieties = [];
      this.filteredVarieties = [];
    }
  }

  private filterCrops(term: string): void {
    if (!term) {
      this.filteredCrops = [...this.allCrops];
    } else {
      this.filteredCrops = this.allCrops.filter(c =>
        c.toLowerCase().includes(term.toLowerCase())
      );
    }
  }

  selectCrop(crop: string): void {
    this.newCrop.cropName = crop;
    this.showCropDropdown = false;
    this.fetchVarieties(crop);
  }

  private fetchVarieties(crop: string): void {
    this.mandiService.getFilterVarieties(crop).subscribe({
      next: (varieties) => {
        this.allVarieties = varieties;
        this.filteredVarieties = [...varieties];
      },
      error: (err) => console.error('Failed to fetch varieties:', err)
    });
  }

  onVarietySearch(term: string | undefined): void {
    const searchTerm = term || '';
    this.showVarietyDropdown = true;
    this.varietiesSearchSubject.next(searchTerm);
  }

  private filterVarieties(term: string): void {
    if (!term) {
      this.filteredVarieties = [...this.allVarieties];
    } else {
      this.filteredVarieties = this.allVarieties.filter(v =>
        v.toLowerCase().includes(term.toLowerCase())
      );
    }
  }

  selectVariety(variety: string): void {
    this.newCrop.cropVariety = variety;
    this.showVarietyDropdown = false;
  }

  addCrop(): void {
    this.editingCropId = null;
    this.newCrop = this.resetCropForm();
    this.showAddCropModal = true;
  }

  editCrop(crop: CropResponse): void {
    this.editingCropId = crop.id;
    this.newCrop = {
      cropName: crop.cropName,
      cropVariety: crop.cropVariety,
      sowingDate: crop.sowingDate,
      expectedHarvestDate: crop.expectedHarvestDate,
      areaAcres: crop.areaAcres,
      season: crop.season as Season,
      status: crop.status as CropStatus,
      seedCost: crop.seedCost,
      fertilizerCost: crop.fertilizerCost,
      pesticideCost: crop.pesticideCost,
      laborCost: crop.laborCost,
      otherCost: crop.otherCost,
      totalYieldQuintals: crop.totalYieldQuintals,
      qualityGrade: crop.qualityGrade,
      sellingPricePerQuintal: crop.sellingPricePerQuintal,
      mandiName: crop.mandiName,
      totalRevenue: crop.totalRevenue,
      actualHarvestDate: crop.actualHarvestDate,
      notes: crop.notes
    };
    this.showAddCropModal = true;
  }

  closeModal(): void {
    this.showAddCropModal = false;
  }

  submitCrop(): void {
    if (!this.newCrop.cropName || !this.newCrop.sowingDate || !this.newCrop.areaAcres) {
      alert('Please fill all required fields');
      return;
    }

    this.isSubmitting = true;

    if (this.editingCropId) {
      this.profileService.updateCrop(this.editingCropId, this.newCrop).subscribe({
        next: (response) => {
          const index = this.crops.findIndex(c => c.id === this.editingCropId);
          if (index !== -1) {
            this.crops[index] = response;
          }
          this.finalizeSubmit();
        },
        error: (error) => this.handleError('Failed to update crop', error)
      });
    } else {
      this.profileService.addCrop(this.newCrop).subscribe({
        next: (response) => {
          this.crops.unshift(response);
          this.finalizeSubmit();
        },
        error: (error) => this.handleError('Failed to add crop', error)
      });
    }
  }

  private finalizeSubmit(): void {
    this.showAddCropModal = false;
    this.isSubmitting = false;
    this.newCrop = this.resetCropForm();
    this.editingCropId = null;
  }

  private handleError(message: string, error: any): void {
    console.error(message, error);
    alert(`${message}. Please try again.`);
    this.isSubmitting = false;
  }

  recordHarvest(): void {
    console.log('Record harvest');
  }

  checkWeather(): void {
    console.log('Check weather');
  }

  viewSchemes(): void {
    console.log('View schemes');
  }
}
