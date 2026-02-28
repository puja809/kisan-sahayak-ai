import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

interface DiseaseDetectionResult {
  id: number;
  diseaseName: string;
  diseaseNameLocal: string;
  confidenceScore: number;
  severityLevel: string;
  affectedAreaPercent: number;
  treatmentRecommendations: TreatmentRecommendation[];
  detectionTimestamp: string;
  imageUrl: string;
}

interface TreatmentRecommendation {
  type: string;
  description: string;
  dosage: string;
  applicationTiming: string;
  estimatedCost: number;
  safetyPrecautions: string;
}

@Component({
  selector: 'app-disease-detection',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="disease-detection-container">
      <div class="header">
        <h1>Disease Detection</h1>
        <p>Upload crop images to identify diseases and get treatment recommendations</p>
      </div>

      <div class="detection-grid">
        <!-- Image Upload Section -->
        <div class="card upload-card">
          <h2>Upload Image</h2>
          <div class="upload-area" (click)="fileInput.click()" [class.dragover]="isDragOver"
               (dragover)="onDragOver($event)" (dragleave)="onDragLeave($event)" (drop)="onDrop($event)">
            <input #fileInput type="file" accept="image/*" (change)="onFileSelected($event)" hidden />
            <div class="upload-content">
              <span class="icon">📸</span>
              <p>Click to upload or drag and drop</p>
              <p class="hint">PNG, JPG up to 10MB</p>
            </div>
          </div>

          <div *ngIf="selectedFile" class="file-preview">
            <p><strong>Selected:</strong> {{ selectedFile.name }}</p>
            <button class="btn-primary" (click)="uploadImage()" [disabled]="isUploading">
              {{ isUploading ? 'Uploading...' : 'Analyze Image' }}
            </button>
          </div>

          <div *ngIf="isAnalyzing" class="analyzing-indicator">
            <p>Analyzing image...</p>
            <div class="progress-bar">
              <div class="progress"></div>
            </div>
          </div>
        </div>

        <!-- Detection Results -->
        <div class="card results-card" *ngIf="detectionResult">
          <h2>Detection Results</h2>
          <div class="result-content">
            <div class="disease-info">
              <h3>{{ detectionResult.diseaseName }}</h3>
              <p class="local-name">{{ detectionResult.diseaseNameLocal }}</p>
              
              <div class="confidence-score">
                <span>Confidence:</span>
                <span class="score" [class]="'score-' + getConfidenceLevel(detectionResult.confidenceScore)">
                  {{ (detectionResult.confidenceScore * 100).toFixed(1) }}%
                </span>
              </div>

              <div class="severity">
                <span>Severity:</span>
                <span class="severity-badge" [class]="'severity-' + detectionResult.severityLevel.toLowerCase()">
                  {{ detectionResult.severityLevel }}
                </span>
              </div>

              <div class="affected-area">
                <span>Affected Area:</span>
                <span>{{ detectionResult.affectedAreaPercent }}%</span>
              </div>
            </div>

            <div class="image-preview" *ngIf="detectionResult.imageUrl">
              <img [src]="detectionResult.imageUrl" alt="Detected disease" />
            </div>
          </div>
        </div>

        <!-- Treatment Recommendations -->
        <div class="card treatments-card" *ngIf="detectionResult && detectionResult.treatmentRecommendations.length > 0">
          <h2>Treatment Recommendations</h2>
          <div class="treatments-list">
            <div *ngFor="let treatment of detectionResult.treatmentRecommendations" class="treatment-item">
              <h3>{{ treatment.type }}</h3>
              <p><strong>Description:</strong> {{ treatment.description }}</p>
              <p><strong>Dosage:</strong> {{ treatment.dosage }}</p>
              <p><strong>Application Timing:</strong> {{ treatment.applicationTiming }}</p>
              <p><strong>Estimated Cost:</strong> ₹{{ treatment.estimatedCost }}</p>
              <p><strong>Safety Precautions:</strong> {{ treatment.safetyPrecautions }}</p>
            </div>
          </div>
        </div>

        <!-- KVK Expert Links -->
        <div class="card expert-card" *ngIf="detectionResult">
          <h2>Expert Consultation</h2>
          <p>For detailed guidance, contact your nearest KVK (Krishi Vigyan Kendra):</p>
          <button class="btn-secondary" (click)="findNearbyKVK()">
            Find Nearby KVK
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .disease-detection-container {
      padding: 1.5rem;
      max-width: 1200px;
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

    .header p {
      color: #666;
      font-size: 1.1rem;
    }

    .detection-grid {
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

    .upload-area {
      border: 2px dashed #667eea;
      border-radius: 8px;
      padding: 2rem;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;
      background: #f8f9fa;
    }

    .upload-area:hover {
      background: #f0f4ff;
      border-color: #5568d3;
    }

    .upload-area.dragover {
      background: #e3f2fd;
      border-color: #2196f3;
    }

    .upload-content .icon {
      font-size: 3rem;
      display: block;
      margin-bottom: 1rem;
    }

    .upload-content p {
      margin: 0.5rem 0;
      color: #666;
    }

    .hint {
      font-size: 0.85rem;
      color: #999;
    }

    .file-preview {
      margin-top: 1rem;
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
    }

    .file-preview p {
      margin: 0.5rem 0;
      color: #666;
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
      margin-top: 1rem;
    }

    .btn-primary:hover:not(:disabled) {
      background: #5568d3;
    }

    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .analyzing-indicator {
      margin-top: 1rem;
      padding: 1rem;
      background: #e3f2fd;
      border-radius: 4px;
      text-align: center;
    }

    .progress-bar {
      width: 100%;
      height: 4px;
      background: #ddd;
      border-radius: 2px;
      margin-top: 0.5rem;
      overflow: hidden;
    }

    .progress {
      height: 100%;
      background: #667eea;
      animation: progress 2s infinite;
    }

    @keyframes progress {
      0% { width: 0; }
      50% { width: 100%; }
      100% { width: 0; }
    }

    .result-content {
      display: grid;
      gap: 1rem;
    }

    .disease-info {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
    }

    .disease-info h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .local-name {
      color: #666;
      font-size: 0.9rem;
      margin-bottom: 1rem;
    }

    .confidence-score,
    .severity,
    .affected-area {
      display: flex;
      justify-content: space-between;
      margin: 0.75rem 0;
      color: #666;
    }

    .score {
      font-weight: 600;
      font-size: 1.1rem;
    }

    .score-high {
      color: #27ae60;
    }

    .score-medium {
      color: #f39c12;
    }

    .score-low {
      color: #e74c3c;
    }

    .severity-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 600;
      color: white;
    }

    .severity-low {
      background: #3498db;
    }

    .severity-medium {
      background: #f39c12;
    }

    .severity-high {
      background: #e74c3c;
    }

    .severity-critical {
      background: #c0392b;
    }

    .image-preview {
      text-align: center;
    }

    .image-preview img {
      max-width: 100%;
      height: auto;
      border-radius: 4px;
      max-height: 300px;
    }

    .treatments-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .treatment-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .treatment-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .treatment-item p {
      margin: 0.5rem 0;
      color: #666;
      font-size: 0.9rem;
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

    @media (max-width: 768px) {
      .detection-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DiseaseDetectionComponent implements OnInit {
  selectedFile: File | null = null;
  isDragOver = false;
  isUploading = false;
  isAnalyzing = false;
  detectionResult: DiseaseDetectionResult | null = null;

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) { }

  ngOnInit(): void {
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      if (this.selectedFile.size > 10 * 1024 * 1024) {
        this.toastr.error('File size must be less than 10MB');
        this.selectedFile = null;
      }
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.selectedFile = event.dataTransfer.files[0];
    }
  }

  uploadImage(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.isAnalyzing = true;
    const formData = new FormData();
    formData.append('image', this.selectedFile);

    this.http.post<DiseaseDetectionResult>('/api/v1/ai/disease/detect', formData).subscribe({
      next: (result) => {
        this.isUploading = false;
        this.isAnalyzing = false;
        this.detectionResult = result;
        this.toastr.success('Disease detection completed');
        this.selectedFile = null;
      },
      error: (error) => {
        this.isUploading = false;
        this.isAnalyzing = false;
        this.toastr.error('Failed to analyze image');
      }
    });
  }

  getConfidenceLevel(score: number): string {
    if (score >= 0.8) return 'high';
    if (score >= 0.6) return 'medium';
    return 'low';
  }

  findNearbyKVK(): void {
    // Navigate to KVK locator
    console.log('Finding nearby KVK');
  }
}
