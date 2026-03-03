import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeolocationService } from '../../services/geolocation.service';
import { CropRecommendationService, DashboardResponse, CropDTO } from '../../services/crop-recommendation.service';

@Component({
  selector: 'app-crops',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="crops-container">
      <h2>🌾 Crop Recommendation Dashboard</h2>
      
      <!-- Tabs Navigation -->
      <div class="tabs" *ngIf="dashboardData">
        <button class="tab-btn" [class.active]="activeTab === 'recommendation'" (click)="switchTab('recommendation')">🌱 Recommendation</button>
        <button class="tab-btn" [class.active]="activeTab === 'fertilizer'" (click)="switchTab('fertilizer')">🧪 Fertilizer</button>
        <button class="tab-btn" [class.active]="activeTab === 'rotation'" (click)="switchTab('rotation')">🔄 Rotation</button>
      </div>

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
      
      <!-- TAB 1: Crop Recommendation Section -->
      <div *ngIf="activeTab === 'recommendation'">
        <div *ngIf="dashboardData?.cropRecommendation" class="prediction-card crop-card">
        <div class="card-header">
          <h3>🌱 Crop Recommendation</h3>
          <span class="model-badge">ML v{{ dashboardData?.cropRecommendation?.modelVersion }}</span>
        </div>
        <div class="card-content">
          <div class="prediction-main">
            <div class="crop-name clickable-crop" (click)="viewCropDetails(dashboardData?.cropRecommendation?.prediction || '')" title="View Crop Details">{{ dashboardData?.cropRecommendation?.prediction }}</div>
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
              <div *ngFor="let crop of getTopProbabilities(dashboardData?.cropRecommendation?.probabilities || {})" class="prob-item clickable-crop" (click)="viewCropDetails(crop.name)" title="View Crop Details">
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

      </div> <!-- End Tab 1 -->

      <!-- TAB 3: Crop Rotation Section -->
      <div *ngIf="activeTab === 'rotation'">
        <div class="rotation-section">
        <h3>🔄 Crop Rotation Recommendation</h3>
        <div class="rotation-form">
          <div class="form-group">
            <label>Previous Crop</label>
            <select [(ngModel)]="previousCrop" class="rotation-select">
              <option value="">Select Previous Crop</option>
              <option *ngFor="let c of availableCrops" [value]="c">{{ c }}</option>
            </select>
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
      </div> <!-- End Tab 3 -->

      <!-- TAB 2: Fertilizer Recommendation Section -->
      <div *ngIf="activeTab === 'fertilizer'">
        <div *ngIf="dashboardData?.fertilizerRecommendation" class="prediction-card fertilizer-card">
        <div class="card-header">
          <h3>🧪 Fertilizer Recommendation</h3>
          <span class="model-badge">ML v{{ dashboardData?.fertilizerRecommendation?.modelVersion }}</span>
        </div>
          <div class="card-content" style="display:flex; flex-direction:column; gap: 1rem;">
            <div class="fertilizer-header" style="display:flex; align-items:center; gap: 1rem;">
              <label style="font-weight:600; color:#E65100;">Target Crop:</label>
              <select [(ngModel)]="selectedFertilizerCrop" (change)="onFertilizerCropChange()" style="padding: 0.5rem; border-radius: 4px; border: 1px solid #FFB74D;">
                <option *ngFor="let c of availableCrops" [value]="c">{{ c }}</option>
              </select>
              <span *ngIf="isCalculatingFertilizer" style="color: #666; font-size: 0.9rem;">⏳ Calculating...</span>
            </div>
            <div class="fertilizer-grid" [class.blur-content]="isCalculatingFertilizer">
              <div class="fertilizer-item">
                <div class="nutrient-label">Nitrogen (N)</div>
                <div class="nutrient-value">{{ currentFertilizer.N | number:'1.1-1' }}</div>
                <div class="nutrient-unit">kg/ha</div>
              </div>
              <div class="fertilizer-item">
                <div class="nutrient-label">Phosphorus (P)</div>
                <div class="nutrient-value">{{ currentFertilizer.P | number:'1.1-1' }}</div>
                <div class="nutrient-unit">kg/ha</div>
              </div>
              <div class="fertilizer-item">
                <div class="nutrient-label">Potassium (K)</div>
                <div class="nutrient-value">{{ currentFertilizer.K | number:'1.1-1' }}</div>
                <div class="nutrient-unit">kg/ha</div>
              </div>
              <div class="fertilizer-item total">
                <div class="nutrient-label">Total NPK</div>
                <div class="nutrient-value">{{ currentFertilizer.total | number:'1.1-1' }}</div>
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
      </div> <!-- End Tab 2 -->

      <p *ngIf="!loading && !dashboardData" class="no-data">No data available. Please enable location access.</p>
      <p *ngIf="loading" class="loading">⏳ Loading dashboard data...</p>

      <!-- Crop Details Modal -->
      <div class="modal-overlay" *ngIf="showCropModal" (click)="closeCropModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <button class="close-btn" (click)="closeCropModal()">×</button>
          
          <div *ngIf="isLoadingCropDetails" class="loading-modal">
            <p>⏳ Loading details for {{ selectedFertilizerCrop }}...</p>
          </div>
          
          <div *ngIf="!isLoadingCropDetails && selectedCropDetails" class="crop-details">
            <h2>{{ selectedCropDetails.commodity }}</h2>
            <div class="detail-badge-container">
              <span class="detail-badge category" *ngIf="selectedCropDetails.category">{{ selectedCropDetails.category }}</span>
              <span class="detail-badge season" *ngIf="selectedCropDetails.season">{{ selectedCropDetails.season }}</span>
            </div>
            
            <div class="crop-meta-grid">
              <div class="meta-item">
                <span class="meta-icon">⏱️</span>
                <div class="meta-text">
                  <label>Duration</label>
                  <span>{{ selectedCropDetails.durationDays }} Days</span>
                </div>
              </div>
              <div class="meta-item">
                <span class="meta-icon">🌱</span>
                <div class="meta-text">
                  <label>Seed Rate</label>
                  <span>{{ selectedCropDetails.seedRateKgPerAcre }}</span>
                </div>
              </div>
              <div class="meta-item">
                <span class="meta-icon">📏</span>
                <div class="meta-text">
                  <label>Spacing</label>
                  <span>{{ selectedCropDetails.spacingCm }}</span>
                </div>
              </div>
              <div class="meta-item">
                <span class="meta-icon">💧</span>
                <div class="meta-text">
                  <label>Irrigation</label>
                  <span>{{ selectedCropDetails.irrigationNumber }}</span>
                </div>
              </div>
              <div class="meta-item">
                <span class="meta-icon">⚖️</span>
                <div class="meta-text">
                  <label>Expected Yield</label>
                  <span>{{ selectedCropDetails.yieldKgPerAcre }}</span>
                </div>
              </div>
            </div>
            
            <div class="detail-section" *ngIf="selectedCropDetails.keyOperations">
              <h4>Key Operations</h4>
              <p>{{ selectedCropDetails.keyOperations }}</p>
            </div>
            
            <div class="detail-section" *ngIf="selectedCropDetails.harvestSigns">
              <h4>Harvest Signs</h4>
              <p>{{ selectedCropDetails.harvestSigns }}</p>
            </div>

            <div class="modal-actions">
              <button class="btn-primary" (click)="navigateToFertilizerFromModal()">
                View Fertilizer Needs
              </button>
            </div>
          </div>
          
          <div *ngIf="!isLoadingCropDetails && !selectedCropDetails" class="no-data-modal">
            <p>Detailed metadata for {{ selectedFertilizerCrop }} is currently unavailable.</p>
            <div class="modal-actions" style="justify-content: center; margin-top: 1.5rem;">
              <button class="btn-primary" (click)="navigateToFertilizerFromModal()">
                View Fertilizer Needs Instead
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Modal Styles */
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
      backdrop-filter: blur(4px);
    }
    .modal-content {
      background: white;
      padding: 2.5rem;
      border-radius: 12px;
      max-width: 700px;
      width: 90%;
      max-height: 85vh;
      overflow-y: auto;
      position: relative;
      box-shadow: 0 10px 30px rgba(0,0,0,0.2);
    }
    .close-btn {
      position: absolute;
      top: 1rem; right: 1.5rem;
      font-size: 2rem;
      background: none;
      border: none;
      cursor: pointer;
      color: #757575;
      line-height: 1;
      padding: 0;
      transition: color 0.2s;
    }
    .close-btn:hover { color: #333; }
    
    .crop-details h2 { margin-bottom: 0.5rem; text-align: left; color: #2E7D32; font-size: 2rem; }
    .detail-badge-container { display: flex; gap: 0.5rem; margin-bottom: 2rem; }
    .detail-badge {
      padding: 0.35rem 1rem; border-radius: 20px; font-size: 0.85rem; font-weight: 600;
    }
    .detail-badge.category { background: #E3F2FD; color: #1565C0; }
    .detail-badge.season { background: #FFF3E0; color: #E65100; }
    
    .crop-meta-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; margin-bottom: 2rem;
    }
    .meta-item {
      display: flex; gap: 1rem; background: #fafafa; padding: 1.25rem; border-radius: 10px; border: 1px solid #eee;
      align-items: center; transition: transform 0.2s, box-shadow 0.2s;
    }
    .meta-item:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
    .meta-icon { font-size: 1.8rem; }
    .meta-text label { display: block; font-size: 0.85rem; color: #757575; margin-bottom: 0.25rem; text-transform: uppercase; letter-spacing: 0.5px;}
    .meta-text span { font-weight: 700; color: #333; font-size: 1.1rem; }
    
    .detail-section { margin-bottom: 1.5rem; background: #E8F5E9; padding: 1.5rem; border-radius: 10px; border-left: 4px solid #2E7D32; }
    .detail-section h4 { color: #1B5E20; margin-bottom: 0.75rem; font-size: 1.1rem; margin-top: 0; }
    .detail-section p { color: #444; line-height: 1.6; margin: 0; }
    
    .modal-actions { display: flex; justify-content: flex-end; margin-top: 2rem; }
    .loading-modal, .no-data-modal { text-align: center; padding: 4rem 0; color: #666; font-size: 1.1rem; }
    
    .rotation-select {
      width: 100%; padding: 0.75rem; border: 1px solid #ccc; border-radius: 6px; background: white;
      font-size: 1rem; transition: border-color 0.2s; font-family: inherit; margin-top: 0.5rem;
    }
    .rotation-select:focus { outline: none; border-color: #2E7D32; box-shadow: 0 0 0 2px rgba(46,125,50,0.2); }

    .crops-container {
      padding: 1.5rem;
      max-width: 1400px;
      margin: 0 auto;
      font-family: 'Inter', 'Segoe UI', sans-serif;
    }
    
    h2 {
      color: #2E7D32;
      margin-bottom: 1.5rem;
      font-size: 2.2rem;
      text-align: center;
      font-weight: 700;
    }

    .tabs {
      display: flex;
      justify-content: center;
      gap: 1rem;
      margin-bottom: 2.5rem;
      border-bottom: 2px solid #E8F5E9;
      padding-bottom: 1rem;
    }

    .tab-btn {
      background: #f9f9f9;
      border: 1px solid #e0e0e0;
      font-size: 1.1rem;
      color: #616161;
      padding: 0.75rem 2rem;
      cursor: pointer;
      font-weight: 600;
      border-radius: 30px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .tab-btn:hover {
      background: #E8F5E9;
      color: #2E7D32;
      border-color: #2E7D32;
      transform: translateY(-2px);
    }

    .tab-btn.active {
      background: #2E7D32;
      color: white;
      border-color: #2E7D32;
      box-shadow: 0 4px 12px rgba(46, 125, 50, 0.3);
    }

    h3 {
      color: #1B5E20;
      margin-bottom: 1rem;
      font-size: 1.4rem;
      font-weight: 700;
    }

    h4 {
      color: #2E7D32;
      margin-bottom: 1rem;
      font-size: 1.1rem;
    }

    .info-banner {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-around;
      gap: 1rem;
      background: linear-gradient(135deg, #ffffff 0%, #F1F8E9 100%);
      padding: 1.5rem;
      border-radius: 12px;
      margin-bottom: 2.5rem;
      border: 1px solid #C8E6C9;
      border-left: 6px solid #2E7D32;
      box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    }

    .info-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.5rem 1rem;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.02);
    }

    .clickable-crop {
      cursor: pointer;
      transition: all 0.2s ease;
      padding: 0.5rem;
      border-radius: 6px;
      margin: -0.5rem;
    }
    .clickable-crop:hover {
      background-color: #f3e5f5;
      transform: scale(1.02);
    }

    .prediction-card {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      margin-bottom: 2rem;
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
      transition: box-shadow 0.3s ease;
    }
    .prediction-card:hover {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    .info-item .label {
      font-weight: 600;
      color: #1B5E20;
    }

    .info-item .value {
      color: #2E7D32;
      font-size: 1.1rem;
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
