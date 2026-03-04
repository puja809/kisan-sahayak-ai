import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeolocationService } from '../../services/geolocation.service';
import { CropRecommendationService, DashboardResponse, CropDTO } from '../../services/crop-recommendation.service';

@Component({
  selector: 'app-crops',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './crops.component.html',
  styleUrls: ['./crops.component.css'],})
export class CropsComponent implements OnInit {
  dashboardData: DashboardResponse | null = null;
  loading = true;
  previousCrop = '';
  selectedSeason = '';
  currentLocation: any = null;

  // Modal State
  selectedCropDetails: CropDTO | null = null;
  showCropModal = false;
  isLoadingCropDetails = false;

  activeTab: 'recommendation' | 'fertilizer' | 'rotation' = 'recommendation';
  selectedFertilizerCrop: string = '';
  availableCrops: string[] = [
    'Apple', 'Banana', 'Blackgram', 'Chickpea', 'Coconut', 'Coffee', 'Cotton', 'Grapes', 'Jute',
    'Kidneybeans', 'Lentil', 'Maize', 'Mango', 'Mothbeans', 'Mungbean', 'Muskmelon', 'Orange',
    'Papaya', 'Pigeonpeas', 'Pomegranate', 'Rice', 'Watermelon'
  ].sort();
  currentFertilizer: { N: number; P: number; K: number; total: number } = { N: 0, P: 0, K: 0, total: 0 };
  isCalculatingFertilizer = false;
  constructor(
    private cropRecommendationService: CropRecommendationService,
    private geolocationService: GeolocationService
  ) { }

  switchTab(tab: 'recommendation' | 'fertilizer' | 'rotation') {
    this.activeTab = tab;
  }

  viewCropDetails(cropName: string) {
    if (!cropName) return;
    this.selectedFertilizerCrop = cropName; // Save choice for fertilizer link
    this.isLoadingCropDetails = true;
    this.showCropModal = true;
    this.selectedCropDetails = null;

    this.cropRecommendationService.getCropDetails(cropName).subscribe({
      next: (details) => {
        this.selectedCropDetails = details;
        this.isLoadingCropDetails = false;
      },
      error: (err) => {
        console.error('Failed to get crop details', err);
        this.isLoadingCropDetails = false;
      }
    });
  }

  closeCropModal() {
    this.showCropModal = false;
  }

  navigateToFertilizerFromModal() {
    this.closeCropModal();
    this.onFertilizerCropChange();
    this.switchTab('fertilizer');
  }

  selectCropForFertilizer(cropName: string) {
    this.selectedFertilizerCrop = cropName;
    this.onFertilizerCropChange();
    this.switchTab('fertilizer');
  }

  onFertilizerCropChange() {
    if (!this.dashboardData || !this.selectedFertilizerCrop) return;

    this.isCalculatingFertilizer = true;

    // Build the request payload matching FertilizerRecommendationRequest in ml_service.py
    const requestBody = {
      crop: this.selectedFertilizerCrop,
      soilType: this.dashboardData.soilData?.soil_type?.texture_class || 'Loam', // Fallback type
      soilPH: this.dashboardData.soilData?.chemical_properties?.ph_h2o || 7.0, // Fallback pH
      temperature: this.dashboardData.weatherData?.current?.temp_c || 25,
      humidity: this.dashboardData.weatherData?.current?.humidity || 50,
      rainfall: this.dashboardData.weatherData?.current?.precip_mm || 100, // Fallback precipitation
      season: this.getSeasonFromMonth() // Calculate derived season from current date
    };

    this.cropRecommendationService.predictFertilizer(requestBody).subscribe({
      next: (modelFertilizer) => {
        this.currentFertilizer = {
          N: modelFertilizer.N_dosage,
          P: modelFertilizer.P_dosage,
          K: modelFertilizer.K_dosage,
          total: modelFertilizer.total_dosage
        };
        this.isCalculatingFertilizer = false;
      },
      error: (err) => {
        console.error('Failed to calculate fertilizer for new crop', err);
        // Fallback to zero if backend fails
        this.currentFertilizer = { N: 0, P: 0, K: 0, total: 0 };
        this.isCalculatingFertilizer = false;
      }
    });
  }

  private getSeasonFromMonth(): string {
    const month = new Date().getMonth() + 1; // 1-12
    if (month >= 6 && month <= 10) return 'Kharif';
    if (month >= 11 || month <= 2) return 'Rabi';
    return 'Summer';
  }

  ngOnInit(): void {
    this.loadAvailableCrops();
    this.loadDashboardData();
  }

  private loadAvailableCrops(): void {
    this.cropRecommendationService.getAvailableCrops().subscribe({
      next: (res) => {
        if (res && res.crops && res.crops.length > 0) {
          this.availableCrops = res.crops;
        }
      },
      error: (err) => {
        console.error('Failed to load dynamic crops list, using local fallback:', err);
      }
    });
  }

  private loadDashboardData(): void {
    this.geolocationService.getCurrentLocation().subscribe({
      next: (location: any) => {
        if (location) {
          this.fetchRecommendations(location.latitude, location.longitude);
        } else {
          console.warn('Geolocation null, falling back to Pune coordinates (18.5204, 73.8567)');
          this.fetchRecommendations(18.5204, 73.8567);
        }
      },
      error: (error: any) => {
        console.error('Failed to get location:', error);
        console.warn('Geolocation denied or failed, falling back to Pune coordinates (18.5204, 73.8567)');
        this.fetchRecommendations(18.5204, 73.8567);
      }
    });
  }

  private fetchRecommendations(lat: number, lng: number): void {
    this.currentLocation = { latitude: lat, longitude: lng };
    this.cropRecommendationService.getDashboardRecommendations(lat, lng).subscribe({
      next: (data: DashboardResponse) => {
        this.dashboardData = data;

        // Fetch location name asynchronously
        this.geolocationService.getAddressFromCoordinates(lat, lng).subscribe({
          next: (address) => {
            if (this.dashboardData && address) {
              this.dashboardData.location = address;
            }
          }
        });

        // Initialize fertilizer tab with the primary recommended crop
        if (data.cropRecommendation?.prediction) {
          this.selectedFertilizerCrop = data.cropRecommendation.prediction;
          // Ensure it's in the list
          if (!this.availableCrops.includes(this.selectedFertilizerCrop)) {
            this.availableCrops.push(this.selectedFertilizerCrop);
            this.availableCrops.sort();
          }
          this.onFertilizerCropChange();
        }

        this.loading = false;
      },
      error: (error: any) => {
        console.error('Failed to load dashboard data:', error);
        this.loading = false;
      }
    });
  }

  getRotationRecommendation(): void {
    if (!this.previousCrop || !this.selectedSeason || !this.currentLocation) {
      alert('Please fill all fields');
      return;
    }

    this.cropRecommendationService.getDashboardRecommendations(
      this.currentLocation.latitude,
      this.currentLocation.longitude,
      this.selectedSeason,
      this.previousCrop
    ).subscribe({
      next: (data: DashboardResponse) => {
        this.dashboardData = data;
      },
      error: (error: any) => {
        console.error('Failed to get rotation recommendation:', error);
        alert('Failed to get rotation recommendation');
      }
    });
  }

  getTopProbabilities(probabilities: { [key: string]: number }): Array<{ name: string; probability: number }> {
    return Object.entries(probabilities)
      .map(([name, probability]) => ({ name, probability }))
      .sort((a, b) => b.probability - a.probability)
      .slice(0, 5);
  }

  getPhRating(ph: number | undefined): string {
    if (!ph) return 'N/A';
    if (ph < 5.5) return 'Acidic';
    if (ph <= 7.5) return 'Neutral';
    return 'Alkaline';
  }
}
