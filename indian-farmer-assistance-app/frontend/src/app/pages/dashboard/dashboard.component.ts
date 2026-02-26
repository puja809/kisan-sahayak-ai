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
  template: `
    <div class="dashboard-container">
      <div class="header">
        <h1>Dashboard</h1>
        <div class="user-info" *ngIf="currentUser">
          <p>Welcome, {{ currentUser.name }}</p>
          <p class="farmer-id">Farmer ID: {{ currentUser.farmerId }}</p>
        </div>
      </div>

      <div class="dashboard-grid">
        <!-- Profile Section -->
        <div class="card profile-card">
          <h2>Profile</h2>
          <div *ngIf="currentUser" class="profile-details">
            <p><strong>Name:</strong> {{ currentUser.name }}</p>
            <p><strong>Phone:</strong> {{ currentUser.phone }}</p>
            <p><strong>Location:</strong> {{ currentUser.district }}, {{ currentUser.state }}</p>
            <p><strong>Language:</strong> {{ currentUser.preferredLanguage }}</p>
          </div>
        </div>

        <!-- Current Crops -->
        <div class="card crops-card">
          <h2>Current Crops</h2>
          <div *ngIf="crops.length > 0" class="crops-list">
            <div *ngFor="let crop of crops" class="crop-item">
              <h3>{{ crop.cropName }}</h3>
              <p>Variety: {{ crop.variety }}</p>
              <p>Area: {{ crop.area }} acres</p>
              <p>Status: <span [class]="'status-' + crop.status.toLowerCase()">{{ crop.status }}</span></p>
            </div>
          </div>
          <p *ngIf="crops.length === 0" class="no-data">No crops recorded yet</p>
          <button class="btn-secondary" (click)="addCrop()">+ Add Crop</button>
        </div>

        <!-- Upcoming Activities -->
        <div class="card activities-card">
          <h2>Upcoming Activities</h2>
          <div *ngIf="activities.length > 0" class="activities-list">
            <div *ngFor="let activity of activities" class="activity-item">
              <p><strong>{{ activity.type }}</strong></p>
              <p>{{ activity.description }}</p>
              <p class="due-date">Due: {{ activity.dueDate | date: 'short' }}</p>
            </div>
          </div>
          <p *ngIf="activities.length === 0" class="no-data">No upcoming activities</p>
        </div>

        <!-- Financial Summary -->
        <div class="card financial-card">
          <h2>Financial Summary</h2>
          <div *ngIf="financialSummary" class="financial-details">
            <div class="financial-item">
              <span>Input Costs:</span>
              <span class="amount">₹{{ financialSummary.totalInputCosts }}</span>
            </div>
            <div class="financial-item">
              <span>Estimated Revenue:</span>
              <span class="amount">₹{{ financialSummary.estimatedRevenue }}</span>
            </div>
            <div class="financial-item profit">
              <span>Profit:</span>
              <span class="amount">₹{{ financialSummary.profit }}</span>
            </div>
          </div>
        </div>

        <!-- Yield Predictions -->
        <div class="card yield-card">
          <h2>Yield Predictions</h2>
          <div *ngIf="yieldPredictions.length > 0" class="yield-list">
            <div *ngFor="let prediction of yieldPredictions" class="yield-item">
              <h3>{{ prediction.cropName }}</h3>
              <p>Expected: {{ prediction.expectedYield }} quintals</p>
              <p>Range: {{ prediction.minYield }} - {{ prediction.maxYield }}</p>
              <p *ngIf="prediction.variance" class="variance">Variance: {{ prediction.variance }}%</p>
            </div>
          </div>
          <p *ngIf="yieldPredictions.length === 0" class="no-data">No yield predictions available</p>
        </div>

        <!-- Quick Actions -->
        <div class="card actions-card">
          <h2>Quick Actions</h2>
          <div class="actions-grid">
            <button class="action-btn" (click)="addCrop()">
              <span class="icon">🌾</span>
              <span>Add Crop</span>
            </button>
            <button class="action-btn" (click)="recordHarvest()">
              <span class="icon">📊</span>
              <span>Record Harvest</span>
            </button>
            <button class="action-btn" (click)="checkWeather()">
              <span class="icon">🌤️</span>
              <span>Check Weather</span>
            </button>
            <button class="action-btn" (click)="viewSchemes()">
              <span class="icon">📋</span>
              <span>View Schemes</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
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
      margin-bottom: 0.5rem;
    }

    .user-info {
      color: #666;
    }

    .farmer-id {
      font-size: 0.9rem;
      color: #999;
    }

    .dashboard-grid {
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

    .profile-details p {
      margin: 0.5rem 0;
      color: #666;
    }

    .crops-list {
      margin-bottom: 1rem;
    }

    .crop-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      margin-bottom: 0.5rem;
    }

    .crop-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .crop-item p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .status-sown {
      color: #3498db;
      font-weight: 600;
    }

    .status-growing {
      color: #f39c12;
      font-weight: 600;
    }

    .status-harvested {
      color: #27ae60;
      font-weight: 600;
    }

    .activities-list {
      margin-bottom: 1rem;
    }

    .activity-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      margin-bottom: 0.5rem;
      border-left: 4px solid #667eea;
    }

    .activity-item p {
      margin: 0.25rem 0;
      color: #666;
    }

    .due-date {
      font-size: 0.85rem;
      color: #999;
    }

    .financial-details {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
    }

    .financial-item {
      display: flex;
      justify-content: space-between;
      margin: 0.75rem 0;
      color: #666;
    }

    .financial-item.profit {
      border-top: 1px solid #ddd;
      padding-top: 0.75rem;
      margin-top: 0.75rem;
      font-weight: 600;
      color: #27ae60;
    }

    .amount {
      font-weight: 600;
      color: #333;
    }

    .yield-list {
      margin-bottom: 1rem;
    }

    .yield-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      margin-bottom: 0.5rem;
    }

    .yield-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .yield-item p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .variance {
      color: #e74c3c;
      font-weight: 600;
    }

    .no-data {
      color: #999;
      font-style: italic;
      margin: 1rem 0;
    }

    .btn-secondary {
      width: 100%;
      padding: 0.75rem;
      background: #6c757d;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
      margin-top: 1rem;
    }

    .btn-secondary:hover {
      background: #5a6268;
    }

    .actions-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.75rem;
    }

    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 1rem;
      background: #f0f4ff;
      border: 2px solid #667eea;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;
      color: #667eea;
      font-weight: 600;
    }

    .action-btn:hover {
      background: #667eea;
      color: white;
    }

    .action-btn .icon {
      font-size: 1.5rem;
      margin-bottom: 0.5rem;
    }

    @media (max-width: 768px) {
      .dashboard-grid {
        grid-template-columns: 1fr;
      }

      .actions-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `]
})
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
