import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { GeolocationService } from '../../services/geolocation.service';

interface SoilData {
  textureClass: string;
  faoClassification: string;
  sandPct: number;
  siltPct: number;
  clayPct: number;
  bulkDensityGCm3: number;
  phH2o: number;
  organicMatterPct: number;
  nitrogenGKg: number;
  cecCmolKg: number;
  capacityFieldVolPct: number;
  capacityWiltVolPct: number;
  latencySeconds: number;
}

interface CropRecommendation {
  rank: number;
  gaezSuitability: any;
  overallSuitabilityScore: number;
  expectedYieldPerAcre: number;
  expectedRevenuePerAcre: number;
  waterRequirementPerAcre: number;
  growingDurationDays: number;
  recommendedVarieties: string[];
  soilHealthRecommendations: string[];
  riskFactors: string[];
  notes: string;
}

interface RecommendationResponse {
  success: boolean;
  recommendations: CropRecommendation[];
  soilData: SoilData;
  location: string;
  agroEcologicalZone: string;
}

@Component({
  selector: 'app-crops',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="crops-container">
      <h2>Crop Recommendations</h2>
      
      <!-- Soil Data Section -->
      <div *ngIf="soilData" class="soil-data-card">
        <h3>Soil Analysis</h3>
        <div class="soil-grid">
          <div class="soil-item">
            <label>Texture Class</label>
            <p>{{ soilData.textureClass }}</p>
          </div>
          <div class="soil-item">
            <label>FAO Classification</label>
            <p>{{ soilData.faoClassification }}</p>
          </div>
          <div class="soil-item">
            <label>pH (H₂O)</label>
            <p>{{ soilData.phH2o | number:'1.2-2' }} ({{ getPhRating(soilData.phH2o) }})</p>
          </div>
          <div class="soil-item">
            <label>Organic Matter</label>
            <p>{{ soilData.organicMatterPct | number:'1.2-2' }}%</p>
          </div>
          <div class="soil-item">
            <label>Nitrogen</label>
            <p>{{ soilData.nitrogenGKg | number:'1.2-2' }} g/kg</p>
          </div>
          <div class="soil-item">
            <label>Water Capacity (Field)</label>
            <p>{{ soilData.capacityFieldVolPct | number:'1.2-2' }}%</p>
          </div>
        </div>
        <div class="soil-texture">
          <h4>Soil Texture Composition</h4>
          <div class="texture-bars">
            <div class="texture-bar">
              <div class="bar sand" [style.width.%]="soilData.sandPct"></div>
              <span>Sand: {{ soilData.sandPct | number:'1.1-1' }}%</span>
            </div>
            <div class="texture-bar">
              <div class="bar silt" [style.width.%]="soilData.siltPct"></div>
              <span>Silt: {{ soilData.siltPct | number:'1.1-1' }}%</span>
            </div>
            <div class="texture-bar">
              <div class="bar clay" [style.width.%]="soilData.clayPct"></div>
              <span>Clay: {{ soilData.clayPct | number:'1.1-1' }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Crop Recommendations -->
      <div *ngIf="recommendations.length > 0" class="recommendations-section">
        <h3>Recommended Crops</h3>
        <div class="recommendations-grid">
          <div *ngFor="let rec of recommendations" class="recommendation-card">
            <div class="rank-badge">Rank {{ rec.rank }}</div>
            <h4>{{ rec.gaezSuitability?.cropName || 'Crop' }}</h4>
            <div class="score-bar">
              <div class="score-fill" [style.width.%]="rec.overallSuitabilityScore"></div>
              <span class="score-text">{{ rec.overallSuitabilityScore | number:'1.0-0' }}% Suitable</span>
            </div>
            <div class="crop-details">
              <p><strong>Expected Yield:</strong> {{ rec.expectedYieldPerAcre | number:'1.1-1' }} quintals/acre</p>
              <p><strong>Water Needed:</strong> {{ rec.waterRequirementPerAcre | number:'1.0-0' }} liters</p>
              <p><strong>Growing Period:</strong> {{ rec.growingDurationDays }} days</p>
              <p><strong>Expected Revenue:</strong> ₹{{ rec.expectedRevenuePerAcre | number:'1.0-0' }}/acre</p>
            </div>
            <div *ngIf="rec.recommendedVarieties && rec.recommendedVarieties.length > 0" class="varieties">
              <strong>Recommended Varieties:</strong>
              <ul>
                <li *ngFor="let variety of rec.recommendedVarieties">{{ variety }}</li>
              </ul>
            </div>
            <div *ngIf="rec.soilHealthRecommendations && rec.soilHealthRecommendations.length > 0" class="recommendations">
              <strong>Soil Health Tips:</strong>
              <ul>
                <li *ngFor="let tip of rec.soilHealthRecommendations">{{ tip }}</li>
              </ul>
            </div>
            <div *ngIf="rec.riskFactors && rec.riskFactors.length > 0" class="risks">
              <strong>Risk Factors:</strong>
              <ul>
                <li *ngFor="let risk of rec.riskFactors">{{ risk }}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <p *ngIf="!loading && recommendations.length === 0" class="no-data">No recommendations available. Please set your location.</p>
      <p *ngIf="loading" class="loading">Loading crop recommendations...</p>
    </div>
  `,
  styles: [`
    .crops-container {
      padding: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
    }
    
    h2 {
      color: #2E7D32;
      margin-bottom: 1.5rem;
      font-size: 1.8rem;
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

    .recommendations-section {
      margin-top: 2rem;
    }

    .recommendations-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1.5rem;
    }

    .recommendation-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border-top: 4px solid #2E7D32;
      position: relative;
    }

    .rank-badge {
      position: absolute;
      top: 1rem;
      right: 1rem;
      background: #2E7D32;
      color: white;
      padding: 0.4rem 0.8rem;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 600;
    }

    .recommendation-card h4 {
      margin-top: 0;
      margin-bottom: 1rem;
      color: #1B5E20;
    }

    .score-bar {
      background: #E8F5E9;
      border-radius: 4px;
      height: 24px;
      margin-bottom: 1rem;
      position: relative;
      overflow: hidden;
    }

    .score-fill {
      background: linear-gradient(90deg, #66BB6A, #2E7D32);
      height: 100%;
      transition: width 0.3s ease;
    }

    .score-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      font-weight: 600;
      color: #1B5E20;
      font-size: 0.85rem;
    }

    .crop-details {
      background: #F5F5F5;
      padding: 1rem;
      border-radius: 6px;
      margin-bottom: 1rem;
    }

    .crop-details p {
      margin: 0.5rem 0;
      font-size: 0.9rem;
      color: #333;
    }

    .varieties, .recommendations, .risks {
      margin-bottom: 1rem;
    }

    .varieties strong, .recommendations strong, .risks strong {
      display: block;
      color: #1B5E20;
      margin-bottom: 0.5rem;
      font-size: 0.9rem;
    }

    .varieties ul, .recommendations ul, .risks ul {
      margin: 0;
      padding-left: 1.5rem;
      font-size: 0.85rem;
      color: #666;
    }

    .varieties li, .recommendations li, .risks li {
      margin: 0.25rem 0;
    }

    .loading {
      text-align: center;
      color: #757575;
      padding: 2rem;
      font-style: italic;
    }

    .no-data {
      text-align: center;
      color: #999;
      padding: 2rem;
      font-style: italic;
    }

    @media (max-width: 768px) {
      .crops-container {
        padding: 1rem;
      }

      .soil-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .recommendations-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CropsComponent implements OnInit {
  recommendations: CropRecommendation[] = [];
  soilData: SoilData | null = null;
  loading = true;

  constructor(
    private http: HttpClient,
    private geolocationService: GeolocationService
  ) {}

  ngOnInit(): void {
    this.loadCropRecommendations();
  }

  private loadCropRecommendations(): void {
    this.geolocationService.getCurrentLocation().subscribe({
      next: (location: any) => {
        if (location) {
          this.http.get<RecommendationResponse>(
            `/api/v1/crops/recommendations?latitude=${location.latitude}&longitude=${location.longitude}`
          ).subscribe({
            next: (data: RecommendationResponse) => {
              if (data.success) {
                this.recommendations = data.recommendations || [];
                this.soilData = data.soilData || null;
              }
              this.loading = false;
            },
            error: (error: any) => {
              console.error('Failed to load recommendations:', error);
              this.loading = false;
            }
          });
        } else {
          this.loading = false;
        }
      },
      error: (error: any) => {
        console.error('Failed to get location:', error);
        this.loading = false;
      }
    });
  }

  getPhRating(ph: number): string {
    if (ph < 5.5) return 'Acidic';
    if (ph <= 7.5) return 'Neutral';
    return 'Alkaline';
  }
}