import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface CropRecommendation {
  cropName: string;
  suitabilityScore: number;
  expectedYield: number;
  waterRequirements: number;
  growingSeasonDays: number;
  potentialYieldGap: number;
}

interface RotationOption {
  id: number;
  crops: string[];
  soilHealthBenefit: number;
  climateResilience: number;
  economicViability: number;
  overallScore: number;
}

interface YieldEstimate {
  cropName: string;
  minYield: number;
  expectedYield: number;
  maxYield: number;
  confidenceInterval: number;
}

interface FertilizerSchedule {
  stage: string;
  fertilizer: string;
  quantity: number;
  applicationDate: string;
  cost: number;
}

@Component({
  selector: 'app-crop-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="crop-container">
      <div class="header">
        <h1>Crop Management</h1>
      </div>

      <div class="crop-grid">
        <!-- Crop Recommendations -->
        <div class="card recommendations-card">
          <h2>Crop Recommendations</h2>
          <div *ngIf="recommendations.length > 0" class="recommendations-list">
            <div *ngFor="let rec of recommendations" class="recommendation-item">
              <h3>{{ rec.cropName }}</h3>
              <p>Suitability Score: {{ (rec.suitabilityScore * 100).toFixed(1) }}%</p>
              <p>Expected Yield: {{ rec.expectedYield }} quintals/acre</p>
              <p>Water Requirements: {{ rec.waterRequirements }} mm</p>
              <p>Growing Season: {{ rec.growingSeasonDays }} days</p>
              <p>Yield Gap: {{ rec.potentialYieldGap }}%</p>
            </div>
          </div>
          <p *ngIf="recommendations.length === 0" class="no-data">No recommendations available</p>
        </div>

        <!-- Crop Rotation Planner -->
        <div class="card rotation-card">
          <h2>Rotation Planner</h2>
          <div *ngIf="rotationOptions.length > 0" class="rotation-list">
            <div *ngFor="let option of rotationOptions" class="rotation-item">
              <h3>{{ option.crops.join(' → ') }}</h3>
              <p>Soil Health Benefit: {{ option.soilHealthBenefit }}%</p>
              <p>Climate Resilience: {{ option.climateResilience }}%</p>
              <p>Economic Viability: {{ option.economicViability }}%</p>
              <p class="overall-score">Overall Score: {{ option.overallScore }}%</p>
            </div>
          </div>
          <p *ngIf="rotationOptions.length === 0" class="no-data">No rotation options available</p>
        </div>

        <!-- Yield Estimator -->
        <div class="card yield-card">
          <h2>Yield Estimator</h2>
          <div *ngIf="yieldEstimates.length > 0" class="yield-list">
            <div *ngFor="let estimate of yieldEstimates" class="yield-item">
              <h3>{{ estimate.cropName }}</h3>
              <p>Min: {{ estimate.minYield }} quintals</p>
              <p>Expected: {{ estimate.expectedYield }} quintals</p>
              <p>Max: {{ estimate.maxYield }} quintals</p>
              <p>Confidence: {{ estimate.confidenceInterval }}%</p>
            </div>
          </div>
          <p *ngIf="yieldEstimates.length === 0" class="no-data">No yield estimates available</p>
        </div>

        <!-- Fertilizer Calculator -->
        <div class="card fertilizer-card">
          <h2>Fertilizer Schedule</h2>
          <div *ngIf="fertilizerSchedule.length > 0" class="schedule-list">
            <div *ngFor="let schedule of fertilizerSchedule" class="schedule-item">
              <p><strong>{{ schedule.stage }}</strong></p>
              <p>Fertilizer: {{ schedule.fertilizer }}</p>
              <p>Quantity: {{ schedule.quantity }} kg/acre</p>
              <p>Date: {{ schedule.applicationDate }}</p>
              <p>Cost: ₹{{ schedule.cost }}</p>
            </div>
          </div>
          <p *ngIf="fertilizerSchedule.length === 0" class="no-data">No schedule available</p>
        </div>

        <!-- Crop History -->
        <div class="card history-card">
          <h2>Crop History (5 Years)</h2>
          <p class="info">Crop history tracking and analysis</p>
        </div>

        <!-- Harvest Recording -->
        <div class="card harvest-card">
          <h2>Record Harvest</h2>
          <div class="harvest-form">
            <input type="text" placeholder="Crop Name" />
            <input type="number" placeholder="Yield (quintals)" />
            <select>
              <option value="">Select Quality Grade</option>
              <option value="A">Grade A</option>
              <option value="B">Grade B</option>
              <option value="C">Grade C</option>
            </select>
            <button class="btn-primary">Record Harvest</button>
          </div>
        </div>

        <!-- Input Cost Tracking -->
        <div class="card costs-card">
          <h2>Input Costs</h2>
          <p class="info">Track seeds, fertilizers, pesticides, and labor costs</p>
        </div>

        <!-- Livestock Management -->
        <div class="card livestock-card">
          <h2>Livestock Management</h2>
          <p class="info">Manage cattle, poultry, and goats</p>
        </div>

        <!-- Equipment Tracking -->
        <div class="card equipment-card">
          <h2>Equipment Tracking</h2>
          <p class="info">Track tractors, harvesters, and pump sets</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .crop-container {
      padding: 1.5rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      margin-bottom: 2rem;
    }

    .header h1 {
      font-size: 2rem;
      color: #333;
      margin: 0;
    }

    .crop-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
    }

    .card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .card h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .recommendations-list,
    .rotation-list,
    .yield-list,
    .schedule-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .recommendation-item,
    .rotation-item,
    .yield-item,
    .schedule-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .recommendation-item h3,
    .rotation-item h3,
    .yield-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .recommendation-item p,
    .rotation-item p,
    .yield-item p,
    .schedule-item p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .overall-score {
      font-weight: 600;
      color: #667eea;
      margin-top: 0.5rem;
    }

    .no-data {
      color: #999;
      font-style: italic;
    }

    .info {
      color: #666;
      font-size: 0.95rem;
    }

    .harvest-form {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .harvest-form input,
    .harvest-form select {
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .harvest-form input:focus,
    .harvest-form select:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .btn-primary {
      padding: 0.75rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }

    .btn-primary:hover {
      background: #5568d3;
    }

    @media (max-width: 768px) {
      .crop-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CropDetailComponent implements OnInit {
  recommendations: CropRecommendation[] = [];
  rotationOptions: RotationOption[] = [];
  yieldEstimates: YieldEstimate[] = [];
  fertilizerSchedule: FertilizerSchedule[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadCropData();
  }

  private loadCropData(): void {
    this.http.get<any>('/api/v1/crops/recommendations').subscribe({
      next: (data) => {
        this.recommendations = data.recommendations || [];
        this.rotationOptions = data.rotationOptions || [];
        this.yieldEstimates = data.yieldEstimates || [];
        this.fertilizerSchedule = data.fertilizerSchedule || [];
      },
      error: (error) => console.error('Failed to load crop data:', error)
    });
  }
}
