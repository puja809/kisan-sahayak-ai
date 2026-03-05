import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

interface DocumentMetadata {
  uploadedBy: string;
  fileFormat: string;
  fileSizeBytes: number;
  s3Key: string;
}

interface Document {
  id: string; // S3 Key
  title: string;
  metadata: DocumentMetadata;
  createdAt: string;
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
  isLoading = false;

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadDocuments();
  }

  private loadDocuments(): void {
    this.isLoading = true;
    this.http.get<Document[]>('/api/v1/admin/documents').subscribe({
      next: (docs) => {
        this.documents = docs;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load documents:', error);
        this.isLoading = false;
        this.toastr.error('Failed to load documents');
      }
    });
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const formData = new FormData();
      formData.append('file', file);
      formData.append('title', file.name);

      this.isLoading = true;
      this.http.post('/api/v1/admin/documents/upload', formData, { responseType: 'text' }).subscribe({
        next: () => {
          this.toastr.success('Document uploaded successfully to S3');
          this.loadDocuments();
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Upload failed:', error);
          this.toastr.error('Failed to upload document');
        }
      });
    }
  }

  downloadDocument(doc: Document): void {
    this.http.get('/api/v1/admin/documents/url', {
      params: { s3Key: doc.metadata.s3Key },
      responseType: 'text'
    }).subscribe({
      next: (url) => {
        window.open(url, '_blank');
      },
      error: (error) => {
        console.error('Failed to get download URL:', error);
        this.toastr.error('Failed to generate download link');
      }
    });
  }

  deleteDocument(doc: Document): void {
    if (confirm(`Are you sure you want to delete "${doc.title}" from S3?`)) {
      this.http.delete('/api/v1/admin/documents', {
        params: { s3Key: doc.metadata.s3Key }
      }).subscribe({
        next: () => {
          this.toastr.success('Document deleted from S3');
          this.loadDocuments();
        },
        error: (error) => {
          console.error('Delete failed:', error);
          this.toastr.error('Failed to delete document');
        }
      });
    }
  }
}

