import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, User } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';

interface Crop {
  id: number;
  cropName: string;
  variety: string;
  sowingDate: string;
  expectedHarvestDate: string;
  area: number;
  status: string;
}

interface Activity {
  id: number;
  type: string;
  description: string;
  dueDate: string;
  priority: string;
}

interface FinancialSummary {
  totalInputCosts: number;
  estimatedRevenue: number;
  profit: number;
}

interface YieldPrediction {
  cropId: number;
  cropName: string;
  minYield: number;
  expectedYield: number;
  maxYield: number;
  variance: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  crops: Crop[] = [];
  activities: Activity[] = [];
  financialSummary: FinancialSummary | null = null;
  yieldPredictions: YieldPrediction[] = [];

  constructor(
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    if (!this.currentUser) return;

    // Load crops
    this.http.get<Crop[]>('/api/v1/users/profile/crops').subscribe({
      next: (data) => this.crops = data,
      error: (error) => console.error('Failed to load crops:', error)
    });

    // Load dashboard data
    this.http.get<any>('/api/v1/users/profile/dashboard').subscribe({
      next: (data) => {
        this.activities = data.upcomingActivities || [];
        this.financialSummary = data.financialSummary;
        this.yieldPredictions = data.yieldPredictions || [];
      },
      error: (error) => console.error('Failed to load dashboard data:', error)
    });
  }

  addCrop(): void {
    // Navigate to crop form
    console.log('Add crop');
  }

  recordHarvest(): void {
    // Navigate to harvest recording
    console.log('Record harvest');
  }

  checkWeather(): void {
    // Navigate to weather page
    console.log('Check weather');
  }

  viewSchemes(): void {
    // Navigate to schemes page
    console.log('View schemes');
  }
}
