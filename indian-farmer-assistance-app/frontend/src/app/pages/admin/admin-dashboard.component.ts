import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

interface Document {
  id: number;
  title: string;
  category: string;
  uploadedBy: string;
  uploadDate: string;
  version: number;
}

interface Scheme {
  id: number;
  schemeCode: string;
  schemeName: string;
  schemeType: string;
  state: string;
  benefitAmount: number;
  isActive: boolean;
}

interface UserAnalytics {
  totalUsers: number;
  activeUsers: number;
  newUsersThisMonth: number;
  recentActions: number;
}

interface AuditLog {
  id: number;
  userId: number;
  action: string;
  entityType: string;
  timestamp: string;
  details: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
})
export class AdminDashboardComponent implements OnInit {
  documents: Document[] = [];
  schemes: Scheme[] = [];
  userAnalytics: UserAnalytics | null = null;
  auditLogs: AuditLog[] = [];
  systemConfig = {
    apiTimeout: 30,
    cacheTTL: 30,
    maxUploadSize: 50
  };

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loadDocuments();
    this.loadSchemes();
    this.loadUserAnalytics();
    this.loadAuditLogs();
  }

  private loadDocuments(): void {
    this.http.get<Document[]>('/api/v1/admin/documents').subscribe({
      next: (docs) => this.documents = docs,
      error: (error) => console.error('Failed to load documents:', error)
    });
  }

  private loadSchemes(): void {
    this.http.get<any>('/api/v1/schemes?page=0&size=10').subscribe({
      next: (response) => this.schemes = response.content || response || [],
      error: (error) => console.error('Failed to load schemes:', error)
    });
  }

  private loadUserAnalytics(): void {
    // Derive analytics from audit logs since no dedicated analytics endpoint exists
    this.http.get<AuditLog[]>('/api/v1/admin/audit/logs').subscribe({
      next: (logs) => {
        const now = new Date();
        const thisMonth = logs.filter(l => new Date(l.timestamp).getMonth() === now.getMonth());
        this.userAnalytics = {
          totalUsers: 0,
          activeUsers: 0,
          newUsersThisMonth: thisMonth.length,
          recentActions: logs.length
        };
      },
      error: (error) => console.error('Failed to load analytics:', error)
    });
  }

  private loadAuditLogs(): void {
    this.http.get<AuditLog[]>('/api/v1/admin/audit/logs').subscribe({
      next: (logs) => this.auditLogs = logs,
      error: (error) => console.error('Failed to load audit logs:', error)
    });
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const formData = new FormData();
      formData.append('file', file);

      this.http.post('/api/v1/admin/documents/upload', formData).subscribe({
        next: () => {
          this.toastr.success('Document uploaded successfully');
          this.loadDocuments();
        },
        error: (error) => this.toastr.error('Failed to upload document')
      });
    }
  }

  editDocument(doc: Document): void {
    console.log('Edit document:', doc);
  }

  deleteDocument(doc: Document): void {
    if (confirm('Are you sure you want to delete this document?')) {
      this.http.delete(`/api/v1/admin/documents/${doc.id}`).subscribe({
        next: () => {
          this.toastr.success('Document deleted');
          this.loadDocuments();
        },
        error: (error) => this.toastr.error('Failed to delete document')
      });
    }
  }

  addScheme(): void {
    console.log('Add scheme');
  }

  editScheme(scheme: Scheme): void {
    console.log('Edit scheme:', scheme);
  }

  deleteScheme(scheme: Scheme): void {
    if (confirm('Are you sure you want to delete this scheme?')) {
      this.http.delete(`/api/v1/admin/schemes/${scheme.id}`).subscribe({
        next: () => {
          this.toastr.success('Scheme deleted');
          this.loadSchemes();
        },
        error: (error) => this.toastr.error('Failed to delete scheme')
      });
    }
  }

  saveConfiguration(): void {
    this.http.post('/api/v1/admin/config', this.systemConfig).subscribe({
      next: () => this.toastr.success('Configuration saved'),
      error: (error) => this.toastr.error('Failed to save configuration')
    });
  }
}
