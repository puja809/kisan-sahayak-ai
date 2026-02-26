import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

interface Scheme {
  id: number;
  schemeCode: string;
  schemeName: string;
  schemeType: string;
  state: string;
  description: string;
  eligibilityCriteria: string;
  benefitAmount: number;
  applicationDeadline: string;
  applicationUrl: string;
}

interface SchemeRecommendation {
  scheme: Scheme;
  eligibilityMatch: number;
  confidenceLevel: string;
  benefitRank: number;
}

interface SchemeApplication {
  id: number;
  schemeId: number;
  schemeName: string;
  status: string;
  applicationDate: string;
  deadline: string;
  documents: string[];
}

@Component({
  selector: 'app-scheme-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="scheme-container">
      <div class="header">
        <h1>Government Schemes</h1>
        <div class="filters">
          <select [(ngModel)]="selectedState" (change)="filterSchemes()">
            <option value="">All States</option>
            <option value="Karnataka">Karnataka</option>
            <option value="Maharashtra">Maharashtra</option>
            <option value="Telangana">Telangana</option>
            <option value="Andhra Pradesh">Andhra Pradesh</option>
            <option value="Haryana">Haryana</option>
            <option value="Uttar Pradesh">Uttar Pradesh</option>
            <option value="Punjab">Punjab</option>
          </select>
        </div>
      </div>

      <div class="scheme-grid">
        <!-- Personalized Recommendations -->
        <div class="card recommendations-card">
          <h2>Recommended for You</h2>
          <div *ngIf="recommendations.length > 0" class="recommendations-list">
            <div *ngFor="let rec of recommendations" class="recommendation-item">
              <h3>{{ rec.scheme.schemeName }}</h3>
              <p>Type: {{ rec.scheme.schemeType }}</p>
              <p>Benefit: ₹{{ rec.scheme.benefitAmount }}</p>
              <p>Eligibility Match: {{ (rec.eligibilityMatch * 100).toFixed(0) }}%</p>
              <p class="confidence" [class]="'confidence-' + rec.confidenceLevel.toLowerCase()">
                {{ rec.confidenceLevel }} Confidence
              </p>
              <button class="btn-primary" (click)="applyScheme(rec.scheme)">Apply Now</button>
            </div>
          </div>
          <p *ngIf="recommendations.length === 0" class="no-data">No recommendations available</p>
        </div>

        <!-- Scheme Browser -->
        <div class="card browser-card">
          <h2>Browse All Schemes</h2>
          <div *ngIf="allSchemes.length > 0" class="schemes-list">
            <div *ngFor="let scheme of allSchemes" class="scheme-item">
              <h3>{{ scheme.schemeName }}</h3>
              <p>Code: {{ scheme.schemeCode }}</p>
              <p>Type: {{ scheme.schemeType }}</p>
              <p>State: {{ scheme.state }}</p>
              <p>Benefit: ₹{{ scheme.benefitAmount }}</p>
              <p class="deadline">Deadline: {{ scheme.applicationDeadline | date: 'short' }}</p>
              <button class="btn-secondary" (click)="viewSchemeDetails(scheme)">View Details</button>
            </div>
          </div>
          <p *ngIf="allSchemes.length === 0" class="no-data">No schemes available</p>
        </div>

        <!-- Eligibility Checker -->
        <div class="card eligibility-card">
          <h2>Eligibility Checker</h2>
          <div class="eligibility-form">
            <select [(ngModel)]="selectedScheme" (change)="checkEligibility()">
              <option value="">Select a Scheme</option>
              <option *ngFor="let scheme of allSchemes" [value]="scheme.id">
                {{ scheme.schemeName }}
              </option>
            </select>
            <div *ngIf="eligibilityResult" class="eligibility-result">
              <p [class]="'result-' + eligibilityResult.eligible">
                {{ eligibilityResult.eligible ? '✓ Eligible' : '✗ Not Eligible' }}
              </p>
              <p>Confidence: {{ eligibilityResult.confidence }}%</p>
              <p>{{ eligibilityResult.reason }}</p>
            </div>
          </div>
        </div>

        <!-- Application Tracker -->
        <div class="card tracker-card">
          <h2>My Applications</h2>
          <div *ngIf="myApplications.length > 0" class="applications-list">
            <div *ngFor="let app of myApplications" class="application-item">
              <h3>{{ app.schemeName }}</h3>
              <p>Status: <span class="status" [class]="'status-' + app.status.toLowerCase()">
                {{ app.status }}
              </span></p>
              <p>Applied: {{ app.applicationDate | date: 'short' }}</p>
              <p>Deadline: {{ app.deadline | date: 'short' }}</p>
              <p>Documents: {{ app.documents.length }} uploaded</p>
              <button class="btn-secondary" (click)="uploadDocuments(app)">Upload Documents</button>
            </div>
          </div>
          <p *ngIf="myApplications.length === 0" class="no-data">No applications yet</p>
        </div>

        <!-- Deadline Notifications -->
        <div class="card deadline-card" *ngIf="upcomingDeadlines.length > 0">
          <h2>Upcoming Deadlines</h2>
          <div class="deadlines-list">
            <div *ngFor="let deadline of upcomingDeadlines" class="deadline-item" [class]="'days-' + getDaysUntilDeadline(deadline.deadline)">
              <h3>{{ deadline.schemeName }}</h3>
              <p>Deadline: {{ deadline.deadline | date: 'short' }}</p>
              <p class="days-left">{{ getDaysUntilDeadline(deadline.deadline) }} days left</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .scheme-container {
      padding: 1.5rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    .header h1 {
      font-size: 2rem;
      color: #333;
      margin: 0;
    }

    .filters select {
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .scheme-grid {
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
    .schemes-list,
    .applications-list,
    .deadlines-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .recommendation-item,
    .scheme-item,
    .application-item,
    .deadline-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .recommendation-item h3,
    .scheme-item h3,
    .application-item h3,
    .deadline-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .recommendation-item p,
    .scheme-item p,
    .application-item p,
    .deadline-item p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .confidence {
      font-weight: 600;
      margin-top: 0.5rem;
    }

    .confidence-high {
      color: #27ae60;
    }

    .confidence-medium {
      color: #f39c12;
    }

    .confidence-low {
      color: #e74c3c;
    }

    .deadline {
      color: #e74c3c;
      font-weight: 600;
    }

    .status {
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.85rem;
      font-weight: 600;
      color: white;
    }

    .status-draft {
      background: #95a5a6;
    }

    .status-submitted {
      background: #3498db;
    }

    .status-approved {
      background: #27ae60;
    }

    .status-rejected {
      background: #e74c3c;
    }

    .days-left {
      font-weight: 600;
      margin-top: 0.5rem;
    }

    .days-1,
    .days-2,
    .days-3,
    .days-4,
    .days-5,
    .days-6,
    .days-7 {
      border-left-color: #e74c3c;
      background: #fadbd8;
    }

    .btn-primary,
    .btn-secondary {
      width: 100%;
      padding: 0.75rem;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
      margin-top: 1rem;
    }

    .btn-primary {
      background: #667eea;
      color: white;
    }

    .btn-primary:hover {
      background: #5568d3;
    }

    .btn-secondary {
      background: #6c757d;
      color: white;
    }

    .btn-secondary:hover {
      background: #5a6268;
    }

    .eligibility-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .eligibility-form select {
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .eligibility-result {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
    }

    .eligibility-result p {
      margin: 0.5rem 0;
      color: #666;
    }

    .result-true {
      color: #27ae60;
      font-weight: 600;
    }

    .result-false {
      color: #e74c3c;
      font-weight: 600;
    }

    .no-data {
      color: #999;
      font-style: italic;
    }

    @media (max-width: 768px) {
      .scheme-grid {
        grid-template-columns: 1fr;
      }

      .header {
        flex-direction: column;
        gap: 1rem;
      }
    }
  `]
})
export class SchemeDetailComponent implements OnInit {
  allSchemes: Scheme[] = [];
  recommendations: SchemeRecommendation[] = [];
  myApplications: SchemeApplication[] = [];
  upcomingDeadlines: any[] = [];
  selectedState = '';
  selectedScheme: number | null = null;
  eligibilityResult: any = null;

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadSchemes();
  }

  private loadSchemes(): void {
    this.http.get<Scheme[]>('/api/v1/schemes').subscribe({
      next: (schemes) => {
        this.allSchemes = schemes;
        this.loadRecommendations();
        this.loadApplications();
      },
      error: (error) => console.error('Failed to load schemes:', error)
    });
  }

  private loadRecommendations(): void {
    this.http.get<SchemeRecommendation[]>('/api/v1/schemes/personalized').subscribe({
      next: (recs) => this.recommendations = recs,
      error: (error) => console.error('Failed to load recommendations:', error)
    });
  }

  private loadApplications(): void {
    this.http.get<SchemeApplication[]>('/api/v1/schemes/applications').subscribe({
      next: (apps) => {
        this.myApplications = apps;
        this.upcomingDeadlines = apps.filter(a => this.getDaysUntilDeadline(a.deadline) <= 7);
      },
      error: (error) => console.error('Failed to load applications:', error)
    });
  }

  filterSchemes(): void {
    if (this.selectedState) {
      this.allSchemes = this.allSchemes.filter(s => s.state === this.selectedState || s.state === 'All');
    } else {
      this.loadSchemes();
    }
  }

  checkEligibility(): void {
    if (!this.selectedScheme) return;

    this.http.post<any>('/api/v1/schemes/eligibility/check', { schemeId: this.selectedScheme }).subscribe({
      next: (result) => this.eligibilityResult = result,
      error: (error) => this.toastr.error('Failed to check eligibility')
    });
  }

  applyScheme(scheme: Scheme): void {
    this.http.post('/api/v1/schemes/applications', { schemeId: scheme.id }).subscribe({
      next: () => {
        this.toastr.success('Application submitted');
        this.loadApplications();
      },
      error: (error) => this.toastr.error('Failed to apply for scheme')
    });
  }

  viewSchemeDetails(scheme: Scheme): void {
    console.log('View scheme details:', scheme);
  }

  uploadDocuments(app: SchemeApplication): void {
    console.log('Upload documents for:', app);
  }

  getDaysUntilDeadline(deadline: string): number {
    const deadlineDate = new Date(deadline);
    const today = new Date();
    const diffTime = deadlineDate.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }
}
