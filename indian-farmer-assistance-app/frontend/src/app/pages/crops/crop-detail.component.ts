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
  templateUrl: './crop-detail.component.html',
  styleUrls: ['./crop-detail.component.css'],})
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
