import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeolocationService } from '../../services/geolocation.service';
import { CropRecommendationService, DashboardResponse } from '../../services/crop-recommendation.service';

@Component({
  selector: 'app-crops',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="crops-container">
      <h2>🌾 Crop Recommendation Dashboard</h2>
      
      <!-- Location & Weather Info -->
      <div *ngIf="dashboardData" class="info-banner">
        <div class="info-item">
          <span class="label">📍 Location:</span>
          <span class="value">{{ dashboardData.location }}</span>
        </div>
        <div class="info-item">
          <span class="label">🌡️ Temperature:</span>
          <span class="value">{{ dashboardData.weatherData.current.temp_c | number:'1.1-1' }}°C</span>
        </div>
        <div class="info-item">
          <span class="label">💧 Humidity:</span>
          <span class="value">{{ dashboardData.weatherData.current.humidity | number:'1.0-0' }}%</span>
        </div>
      </div>
      
      <!-- Soil Data Section (Moved to Top) -->
      <div *ngIf="dashboardData?.soilData" class="soil-data-card">
        <h3>🌍 Soil Analysis</h3>
        <div class="soil-grid">
          <div class="soil-item">
            <label>Texture Class</label>
            <p>{{ dashboardData?.soilData?.soil_type?.texture_class || 'N/A' }}</p>
          </div>
          <div class="soil-item">
            <label>FAO Classification</label>
            <p>{{ dashboardData?.soilData?.soil_type?.fao_class || 'N/A' }}</p>
          </div>
          <div class="soil-item">
            <label>pH (H₂O)</label>
            <p>{{ dashboardData?.soilData?.chemical_properties?.ph_h2o ? (dashboardData?.soilData?.chemical_properties?.ph_h2o | number:'1.2-2') + ' (' + getPhRating(dashboardData?.soilData?.chemical_properties?.ph_h2o) + ')' : 'N/A' }}</p>
          </div>
          <div class="soil-item">
            <label>Organic Matter</label>
            <p>{{ dashboardData?.soilData?.chemical_properties?.organic_matter_pct ? (dashboardData?.soilData?.chemical_properties?.organic_matter_pct | number:'1.2-2') + '%' : 'N/A' }}</p>
          </div>
          <div class="soil-item">
            <label>Nitrogen</label>
            <p>{{ dashboardData?.soilData?.chemical_properties?.nitrogen_g_kg ? (dashboardData?.soilData?.chemical_properties?.nitrogen_g_kg | number:'1.2-2') + ' g/kg' : 'N/A' }}</p>
          </div>
          <div class="soil-item">
            <label>Water Capacity (Field)</label>
            <p>{{ dashboardData?.soilData?.water_metrics?.capacity_field_vol_pct ? (dashboardData?.soilData?.water_metrics?.capacity_field_vol_pct | number:'1.2-2') + '%' : 'N/A' }}</p>
          </div>
        </div>
        <div class="soil-texture" *ngIf="dashboardData?.soilData?.physical_properties">
          <h4>Soil Texture Composition</h4>
          <div class="texture-bars">
            <div class="texture-bar">
              <div class="bar sand" [style.width.%]="dashboardData?.soilData?.physical_properties?.sand_pct || 0"></div>
              <span>Sand: {{ dashboardData?.soilData?.physical_properties?.sand_pct | number:'1.1-1' }}%</span>
            </div>
            <div class="texture-bar">
              <div class="bar silt" [style.width.%]="dashboardData?.soilData?.physical_properties?.silt_pct || 0"></div>
              <span>Silt: {{ dashboardData?.soilData?.physical_properties?.silt_pct | number:'1.1-1' }}%</span>
            </div>
            <div class="texture-bar">
              <div class="bar clay" [style.width.%]="dashboardData?.soilData?.physical_properties?.clay_pct || 0"></div>
              <span>Clay: {{ dashboardData?.soilData?.physical_properties?.clay_pct | number:'1.1-1' }}%</span>
            </div>
          </div>
        </div>
        <div *ngIf="!dashboardData?.soilData?.physical_properties" class="no-texture-data">
          <p>Detailed soil texture composition not available.</p>
        </div>
      </div>
      
      <!-- Crop Recommendation Section -->
      <div *ngIf="dashboardData?.cropRecommendation" class="prediction-card crop-card">
        <div class="card-header">
          <h3>🌱 Crop Recommendation</h3>
          <span class="model-badge">ML v{{ dashboardData?.cropRecommendation?.modelVersion }}</span>
        </div>
        <div class="card-content">
          <div class="prediction-main">
            <div class="crop-name">{{ dashboardData?.cropRecommendation?.prediction }}</div>
            <div class="confidence-meter">
              <div class="confidence-bar">
                <div class="confidence-fill" [style.width.%]="(dashboardData?.cropRecommendation?.confidence || 0) * 100"></div>
              </div>
              <span class="confidence-text">{{ ((dashboardData?.cropRecommendation?.confidence || 0) * 100) | number:'1.1-1' }}% Confidence</span>
            </div>
          </div>
          <div class="probabilities">
            <h4>Alternative Crops</h4>
            <div class="prob-list">
              <div *ngFor="let crop of getTopProbabilities(dashboardData?.cropRecommendation?.probabilities || {})" class="prob-item">
                <span class="crop-name-alt">{{ crop.name }}</span>
                <div class="prob-bar">
                  <div class="prob-fill" [style.width.%]="crop.probability * 100"></div>
                </div>
                <span class="prob-value">{{ (crop.probability * 100) | number:'1.0-0' }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div *ngIf="!dashboardData?.cropRecommendation" class="prediction-card crop-card no-data-card">
        <div class="card-header">
          <h3>🌱 Crop Recommendation</h3>
        </div>
        <div class="card-content">
          <p class="no-data-message">No crop recommendation available. Please check your location and try again.</p>
        </div>
      </div>

      <!-- Crop Rotation Section -->
      <div class="rotation-section">
        <h3>🔄 Crop Rotation Recommendation</h3>
        <div class="rotation-form">
          <div class="form-group">
            <label>Previous Crop</label>
            <input type="text" [(ngModel)]="previousCrop" placeholder="e.g., Wheat, Rice, Cotton">
          </div>
          <div class="form-group">
            <label>Season</label>
            <select [(ngModel)]="selectedSeason">
              <option value="">Select Season</option>
              <option value="Kharif">Kharif</option>
              <option value="Rabi">Rabi</option>
              <option value="Summer">Summer</option>
            </select>
          </div>
          <button (click)="getRotationRecommendation()" class="btn-primary">Get Rotation</button>
        </div>
        <div *ngIf="dashboardData?.cropRotation" class="rotation-result">
          <div class="prediction-card rotation-card">
            <div class="crop-name">{{ dashboardData?.cropRotation?.prediction }}</div>
            <div class="confidence-meter">
              <div class="confidence-bar">
                <div class="confidence-fill" [style.width.%]="(dashboardData?.cropRotation?.confidence || 0) * 100"></div>
              </div>
              <span class="confidence-text">{{ ((dashboardData?.cropRotation?.confidence || 0) * 100) | number:'1.1-1' }}% Confidence</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Fertilizer Recommendation Section -->
      <div *ngIf="dashboardData?.fertilizerRecommendation" class="prediction-card fertilizer-card">
        <div class="card-header">
          <h3>🧪 Fertilizer Recommendation</h3>
          <span class="model-badge">ML v{{ dashboardData?.fertilizerRecommendation?.modelVersion }}</span>
        </div>
        <div class="card-content">
          <div class="fertilizer-grid">
            <div class="fertilizer-item">
              <div class="nutrient-label">Nitrogen (N)</div>
              <div class="nutrient-value">{{ dashboardData?.fertilizerRecommendation?.N_dosage | number:'1.1-1' }}</div>
              <div class="nutrient-unit">kg/ha</div>
            </div>
            <div class="fertilizer-item">
              <div class="nutrient-label">Phosphorus (P)</div>
              <div class="nutrient-value">{{ dashboardData?.fertilizerRecommendation?.P_dosage | number:'1.1-1' }}</div>
              <div class="nutrient-unit">kg/ha</div>
            </div>
            <div class="fertilizer-item">
              <div class="nutrient-label">Potassium (K)</div>
              <div class="nutrient-value">{{ dashboardData?.fertilizerRecommendation?.K_dosage | number:'1.1-1' }}</div>
              <div class="nutrient-unit">kg/ha</div>
            </div>
            <div class="fertilizer-item total">
              <div class="nutrient-label">Total NPK</div>
              <div class="nutrient-value">{{ dashboardData?.fertilizerRecommendation?.total_dosage | number:'1.1-1' }}</div>
              <div class="nutrient-unit">kg/ha</div>
            </div>
          </div>
        </div>
      </div>
      <div *ngIf="!dashboardData?.fertilizerRecommendation" class="prediction-card fertilizer-card no-data-card">
        <div class="card-header">
          <h3>🧪 Fertilizer Recommendation</h3>
        </div>
        <div class="card-content">
          <p class="no-data-message">No fertilizer recommendation available. Crop recommendation is required first.</p>
        </div>
      </div>

      <p *ngIf="!loading && !dashboardData" class="no-data">No data available. Please enable location access.</p>
      <p *ngIf="loading" class="loading">⏳ Loading dashboard data...</p>
    </div>
  `,
  styles: [`
    .crops-container {
      padding: 1.5rem;
      max-width: 1400px;
      margin: 0 auto;
    }
    
    h2 {
      color: #2E7D32;
      margin-bottom: 1.5rem;
      font-size: 2rem;
      text-align: center;
    }

    h3 {
      color: #1B5E20;
      margin-bottom: 1rem;
      font-size: 1.3rem;
    }

    h4 {
      color: #2E7D32;
      margin-bottom: 0.5rem;
    }

    .info-banner {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      background: linear-gradient(135deg, #E8F5E9 0%, #F1F8E9 100%);
      padding: 1.5rem;
      border-radius: 8px;
      margin-bottom: 2rem;
      border-left: 4px solid #2E7D32;
    }

    .info-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .info-item .label {
      font-weight: 600;
      color: #1B5E20;
    }

    .info-item .value {
      color: #2E7D32;
      font-size: 1.1rem;
    }

    .prediction-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      margin-bottom: 2rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .crop-card {
      border-left: 4px solid #7C4DFF;
      background: linear-gradient(135deg, #F3E5F5 0%, #EDE7F6 100%);
    }

    .fertilizer-card {
      border-left: 4px solid #FF6F00;
      background: linear-gradient(135deg, #FFF3E0 0%, #FCE4EC 100%);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .card-header h3 {
      margin: 0;
    }

    .model-badge {
      background: #7C4DFF;
      color: white;
      padding: 0.3rem 0.8rem;
      border-radius: 20px;
      font-size: 0.75rem;
      font-weight: 600;
    }

    .card-content {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }

    .prediction-main {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
    }

    .crop-name {
      font-size: 2rem;
      font-weight: 700;
      color: #7C4DFF;
      margin-bottom: 1rem;
    }

    .confidence-meter {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .confidence-bar {
      background: #E0E0E0;
      border-radius: 4px;
      height: 28px;
      overflow: hidden;
    }

    .confidence-fill {
      background: linear-gradient(90deg, #7C4DFF, #5E35B1);
      height: 100%;
      transition: width 0.3s ease;
    }

    .confidence-text {
      font-weight: 600;
      color: #5E35B1;
      font-size: 0.9rem;
    }

    .probabilities {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
    }

    .probabilities h4 {
      margin-top: 0;
      color: #7C4DFF;
    }

    .prob-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .prob-item {
      display: grid;
      grid-template-columns: 100px 1fr 60px;
      align-items: center;
      gap: 0.75rem;
    }

    .crop-name-alt {
      font-weight: 500;
      color: #333;
      font-size: 0.9rem;
    }

    .prob-bar {
      background: #F0F0F0;
      border-radius: 3px;
      height: 20px;
      overflow: hidden;
    }

    .prob-fill {
      background: linear-gradient(90deg, #9C27B0, #7C4DFF);
      height: 100%;
    }

    .prob-value {
      text-align: right;
      font-weight: 600;
      color: #666;
      font-size: 0.85rem;
    }

    .rotation-section {
      background: linear-gradient(135deg, #FFF3E0 0%, #FCE4EC 100%);
      border-left: 4px solid #FF6F00;
      padding: 1.5rem;
      border-radius: 8px;
      margin-bottom: 2rem;
    }

    .rotation-section h3 {
      color: #FF6F00;
      margin-top: 0;
    }

    .rotation-form {
      display: grid;
      grid-template-columns: 1fr 1fr auto;
      gap: 1rem;
      margin-bottom: 1rem;
      background: white;
      padding: 1rem;
      border-radius: 6px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
    }

    .form-group label {
      font-weight: 600;
      color: #FF6F00;
      margin-bottom: 0.4rem;
      font-size: 0.9rem;
    }

    .form-group input,
    .form-group select {
      padding: 0.6rem;
      border: 1px solid #FFB74D;
      border-radius: 4px;
      font-size: 0.9rem;
    }

    .form-group input:focus,
    .form-group select:focus {
      outline: none;
      border-color: #FF6F00;
      box-shadow: 0 0 0 2px rgba(255, 111, 0, 0.1);
    }

    .btn-primary {
      background: #FF6F00;
      color: white;
      border: none;
      padding: 0.6rem 1.5rem;
      border-radius: 4px;
      font-weight: 600;
      cursor: pointer;
      align-self: flex-end;
      transition: background 0.3s ease;
    }

    .btn-primary:hover {
      background: #E65100;
    }

    .rotation-result {
      background: white;
      padding: 1rem;
      border-radius: 6px;
      margin-top: 1rem;
    }

    .rotation-card {
      background: #FFF9C4;
      padding: 1rem;
      border-radius: 6px;
      border-left: 4px solid #FF6F00;
    }

    .fertilizer-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 1rem;
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
    }

    .fertilizer-item {
      text-align: center;
      padding: 1rem;
      background: #F5F5F5;
      border-radius: 6px;
      border-top: 3px solid #FF6F00;
    }

    .fertilizer-item.total {
      border-top-color: #2E7D32;
      background: #E8F5E9;
      font-weight: 600;
    }

    .nutrient-label {
      font-size: 0.85rem;
      color: #666;
      margin-bottom: 0.5rem;
      font-weight: 600;
    }

    .nutrient-value {
      font-size: 1.8rem;
      font-weight: 700;
      color: #FF6F00;
      margin-bottom: 0.25rem;
    }

    .fertilizer-item.total .nutrient-value {
      color: #2E7D32;
    }

    .nutrient-unit {
      font-size: 0.75rem;
      color: #999;
    }

    .soil-data-card {
      background: linear-gradient(135deg, #E8F5E9 0%, #F1F8E9 100%);
      border-left: 4px solid #2E7D32;
      padding: 1.5rem;
      border-radius: 8px;
      margin-bottom: 2rem;
    }

    .soil-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .soil-item {
      background: white;
      padding: 1rem;
      border-radius: 6px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }

    .soil-item label {
      display: block;
      font-weight: 600;
      color: #1B5E20;
      font-size: 0.85rem;
      margin-bottom: 0.5rem;
    }

    .soil-item p {
      margin: 0;
      color: #333;
      font-size: 1.1rem;
    }

    .soil-texture {
      background: white;
      padding: 1rem;
      border-radius: 6px;
      margin-top: 1rem;
    }

    .soil-texture h4 {
      margin-top: 0;
    }

    .texture-bars {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .texture-bar {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .texture-bar span {
      min-width: 100px;
      font-size: 0.9rem;
      color: #666;
    }

    .bar {
      height: 24px;
      border-radius: 4px;
      min-width: 50px;
    }

    .bar.sand {
      background: linear-gradient(90deg, #FFD54F, #FFC107);
    }

    .bar.silt {
      background: linear-gradient(90deg, #A1887F, #8D6E63);
    }

    .bar.clay {
      background: linear-gradient(90deg, #EF9A9A, #E57373);
    }

    .no-data {
      text-align: center;
      color: #999;
      padding: 2rem;
      font-style: italic;
    }

    .no-data-card {
      background: #f5f5f5;
      border-left-color: #ccc;
    }

    .no-data-message {
      color: #666;
      text-align: center;
      padding: 1rem;
      font-style: italic;
    }

    .no-texture-data {
      background: #f9f9f9;
      padding: 1rem;
      border-radius: 4px;
      color: #999;
      text-align: center;
      font-style: italic;
    }

    .loading {
      text-align: center;
      color: #2E7D32;
      padding: 2rem;
      font-weight: 600;
    }

    @media (max-width: 768px) {
      .crops-container {
        padding: 1rem;
      }

      .soil-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .card-content {
        grid-template-columns: 1fr;
      }

      .rotation-form {
        grid-template-columns: 1fr;
      }

      .btn-primary {
        align-self: flex-start;
      }

      .info-banner {
        grid-template-columns: 1fr;
      }

      .fertilizer-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `]
})
export class CropsComponent implements OnInit {
  dashboardData: DashboardResponse | null = null;
  loading = true;
  previousCrop = '';
  selectedSeason = '';
  currentLocation: any = null;

  constructor(
    private cropRecommendationService: CropRecommendationService,
    private geolocationService: GeolocationService
  ) { }

  ngOnInit(): void {
    this.loadDashboardData();
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
