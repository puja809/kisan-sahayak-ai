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
  averageSessionDuration: number;
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
  template: `
    <div class="admin-dashboard-container">
      <div class="header">
        <h1>Admin Dashboard</h1>
      </div>

      <div class="admin-grid">
        <!-- User Analytics -->
        <div class="card analytics-card">
          <h2>User Analytics</h2>
          <div *ngIf="userAnalytics" class="analytics-content">
            <div class="stat-item">
              <span class="label">Total Users</span>
              <span class="value">{{ userAnalytics.totalUsers }}</span>
            </div>
            <div class="stat-item">
              <span class="label">Active Users</span>
              <span class="value">{{ userAnalytics.activeUsers }}</span>
            </div>
            <div class="stat-item">
              <span class="label">New This Month</span>
              <span class="value">{{ userAnalytics.newUsersThisMonth }}</span>
            </div>
            <div class="stat-item">
              <span class="label">Avg Session Duration</span>
              <span class="value">{{ userAnalytics.averageSessionDuration }}m</span>
            </div>
          </div>
        </div>

        <!-- Document Management -->
        <div class="card documents-card">
          <h2>Document Management</h2>
          <div class="upload-section">
            <input type="file" #fileInput (change)="onDocumentSelected($event)" hidden />
            <button class="btn-primary" (click)="fileInput.click()">
              Upload Document
            </button>
          </div>

          <div *ngIf="documents.length > 0" class="documents-list">
            <div *ngFor="let doc of documents" class="document-item">
              <div class="doc-info">
                <h3>{{ doc.title }}</h3>
                <p>Category: {{ doc.category }}</p>
                <p>Uploaded by: {{ doc.uploadedBy }}</p>
                <p>Version: {{ doc.version }}</p>
              </div>
              <div class="doc-actions">
                <button class="btn-small" (click)="editDocument(doc)">Edit</button>
                <button class="btn-small danger" (click)="deleteDocument(doc)">Delete</button>
              </div>
            </div>
          </div>
          <p *ngIf="documents.length === 0" class="no-data">No documents uploaded</p>
        </div>

        <!-- Scheme Management -->
        <div class="card schemes-card">
          <h2>Scheme Management</h2>
          <button class="btn-primary" (click)="addScheme()">+ Add Scheme</button>

          <div *ngIf="schemes.length > 0" class="schemes-list">
            <div *ngFor="let scheme of schemes" class="scheme-item">
              <div class="scheme-info">
                <h3>{{ scheme.schemeName }}</h3>
                <p>Code: {{ scheme.schemeCode }}</p>
                <p>Type: {{ scheme.schemeType }}</p>
                <p>State: {{ scheme.state }}</p>
                <p>Benefit: ₹{{ scheme.benefitAmount }}</p>
                <p class="status" [class]="scheme.isActive ? 'active' : 'inactive'">
                  {{ scheme.isActive ? 'Active' : 'Inactive' }}
                </p>
              </div>
              <div class="scheme-actions">
                <button class="btn-small" (click)="editScheme(scheme)">Edit</button>
                <button class="btn-small danger" (click)="deleteScheme(scheme)">Delete</button>
              </div>
            </div>
          </div>
          <p *ngIf="schemes.length === 0" class="no-data">No schemes configured</p>
        </div>

        <!-- System Configuration -->
        <div class="card config-card">
          <h2>System Configuration</h2>
          <div class="config-form">
            <div class="form-group">
              <label>API Timeout (seconds)</label>
              <input type="number" [(ngModel)]="systemConfig.apiTimeout" />
            </div>
            <div class="form-group">
              <label>Cache TTL (minutes)</label>
              <input type="number" [(ngModel)]="systemConfig.cacheTTL" />
            </div>
            <div class="form-group">
              <label>Max Upload Size (MB)</label>
              <input type="number" [(ngModel)]="systemConfig.maxUploadSize" />
            </div>
            <button class="btn-primary" (click)="saveConfiguration()">Save Configuration</button>
          </div>
        </div>

        <!-- Audit Logs -->
        <div class="card audit-card">
          <h2>Audit Logs</h2>
          <div *ngIf="auditLogs.length > 0" class="audit-list">
            <div *ngFor="let log of auditLogs" class="audit-item">
              <p><strong>{{ log.action }}</strong></p>
              <p>Entity: {{ log.entityType }}</p>
              <p>User ID: {{ log.userId }}</p>
              <p class="timestamp">{{ log.timestamp | date: 'short' }}</p>
              <p class="details">{{ log.details }}</p>
            </div>
          </div>
          <p *ngIf="auditLogs.length === 0" class="no-data">No audit logs</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-dashboard-container {
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

    .admin-grid {
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

    .analytics-content {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1rem;
    }

    .stat-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      text-align: center;
    }

    .stat-item .label {
      display: block;
      color: #666;
      font-size: 0.85rem;
      margin-bottom: 0.5rem;
    }

    .stat-item .value {
      display: block;
      font-size: 1.75rem;
      font-weight: 600;
      color: #667eea;
    }

    .btn-primary {
      width: 100%;
      padding: 0.75rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
      margin-bottom: 1rem;
    }

    .btn-primary:hover {
      background: #5568d3;
    }

    .upload-section {
      margin-bottom: 1rem;
    }

    .documents-list,
    .schemes-list,
    .audit-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .document-item,
    .scheme-item,
    .audit-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .doc-info h3,
    .scheme-info h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .doc-info p,
    .scheme-info p,
    .audit-item p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .status {
      font-weight: 600;
      margin-top: 0.5rem;
    }

    .status.active {
      color: #27ae60;
    }

    .status.inactive {
      color: #e74c3c;
    }

    .doc-actions,
    .scheme-actions {
      display: flex;
      gap: 0.5rem;
      margin-top: 1rem;
    }

    .btn-small {
      flex: 1;
      padding: 0.5rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .btn-small:hover {
      background: #5568d3;
    }

    .btn-small.danger {
      background: #e74c3c;
    }

    .btn-small.danger:hover {
      background: #c0392b;
    }

    .no-data {
      color: #999;
      font-style: italic;
      margin: 1rem 0;
    }

    .config-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .form-group label {
      color: #333;
      font-weight: 600;
    }

    .form-group input {
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .form-group input:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .timestamp {
      font-size: 0.85rem;
      color: #999;
    }

    .details {
      font-size: 0.85rem;
      color: #667eea;
      margin-top: 0.5rem;
    }

    @media (max-width: 768px) {
      .admin-grid {
        grid-template-columns: 1fr;
      }

      .analytics-content {
        grid-template-columns: 1fr;
      }
    }
  `]
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
  ) {}

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
    this.http.get<Scheme[]>('/api/v1/admin/schemes').subscribe({
      next: (schemes) => this.schemes = schemes,
      error: (error) => console.error('Failed to load schemes:', error)
    });
  }

  private loadUserAnalytics(): void {
    this.http.get<UserAnalytics>('/api/v1/admin/analytics/users').subscribe({
      next: (analytics) => this.userAnalytics = analytics,
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
