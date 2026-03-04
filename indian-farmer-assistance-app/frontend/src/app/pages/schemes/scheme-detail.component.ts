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
  templateUrl: './scheme-detail.component.html',
  styleUrls: ['./scheme-detail.component.css'],})
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
