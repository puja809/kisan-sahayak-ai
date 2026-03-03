import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { DiseaseDetectionService, DiseaseDetectionResult } from '../../services/disease-detection.service';
import { LanguageService, Language } from '../../services/language.service';

@Component({
  selector: 'app-disease-detection',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="disease-detection-container">
      <div class="header">
        <h1>🌿 Disease Detection</h1>
        <p>Upload crop images to identify diseases and get treatment recommendations</p>
      </div>

      <div class="detection-grid">
        <!-- Image Upload Section -->
        <div class="card upload-card">
          <h2>Upload Image</h2>

          <!-- Language Selector -->
          <div class="language-selector">
            <label for="languageSelect">Response Language:</label>
            <select id="languageSelect" [(ngModel)]="selectedLanguage" class="lang-select">
              <option *ngFor="let lang of languages" [value]="lang.code">
                {{ lang.nativeName }} ({{ lang.name }})
              </option>
            </select>
          </div>

          <div class="upload-area" (click)="fileInput.click()" [class.dragover]="isDragOver"
               (dragover)="onDragOver($event)" (dragleave)="onDragLeave($event)" (drop)="onDrop($event)">
            <input #fileInput type="file" accept="image/*" (change)="onFileSelected($event)" hidden />
            <div class="upload-content">
              <span class="icon">📸</span>
              <p>Click to upload or drag and drop</p>
              <p class="hint">PNG, JPG, WebP up to 10MB</p>
            </div>
          </div>

          <!-- Image Preview -->
          <div *ngIf="previewUrl" class="image-preview-box">
            <img [src]="previewUrl" alt="Crop image preview" class="image-preview" />
          </div>

          <div *ngIf="selectedFile" class="file-preview">
            <p><strong>Selected:</strong> {{ selectedFile.name }}</p>
            <button class="btn-primary" (click)="uploadImage()" [disabled]="isUploading">
              {{ isUploading ? 'Analyzing...' : 'Analyze Image' }}
            </button>
          </div>

          <div *ngIf="isAnalyzing" class="analyzing-indicator">
            <p>🔬 Analyzing crop image with AI...</p>
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
              <h3>{{ detectionResult.disease || 'Analysis Complete' }}</h3>
              <p class="crop-name"><strong>Crop:</strong> {{ detectionResult.crop || 'See analysis below' }}</p>

              <div class="confidence-score" *ngIf="detectionResult.confidence > 0">
                <span>Confidence:</span>
                <span class="score" [class]="'score-' + getConfidenceLevel(detectionResult.confidence)">
                  {{ (detectionResult.confidence * 100).toFixed(1) }}%
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Symptoms -->
        <div class="card symptoms-card" *ngIf="detectionResult?.symptoms">
          <h2>🩺 Symptoms</h2>
          <p>{{ detectionResult!.symptoms }}</p>
        </div>

        <!-- Treatment -->
        <div class="card treatment-card" *ngIf="detectionResult?.treatment">
          <h2>💊 Treatment</h2>
          <p>{{ detectionResult!.treatment }}</p>
        </div>

        <!-- Prevention -->
        <div class="card prevention-card" *ngIf="detectionResult?.prevention">
          <h2>🛡️ Prevention</h2>
          <p>{{ detectionResult!.prevention }}</p>
        </div>

        <!-- Raw Analysis Fallback (shown when structured parsing has gaps) -->
        <div class="card raw-card"
             *ngIf="detectionResult?.raw_analysis && (!detectionResult?.disease || !detectionResult?.symptoms)">
          <h2>📋 Full Analysis</h2>
          <p class="raw-analysis">{{ detectionResult!.raw_analysis }}</p>
        </div>

        <!-- KVK Expert Links -->
        <div class="card expert-card" *ngIf="detectionResult">
          <h2>👨‍🌾 Expert Consultation</h2>
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

    /* Language selector */
    .language-selector {
      margin-bottom: 1rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .language-selector label {
      font-size: 0.9rem;
      color: #555;
      white-space: nowrap;
    }

    .lang-select {
      flex: 1;
      padding: 0.4rem 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 0.9rem;
      background: #f8f9fa;
      cursor: pointer;
    }

    .lang-select:focus {
      outline: none;
      border-color: #667eea;
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

    /* Image preview */
    .image-preview-box {
      margin-top: 1rem;
      text-align: center;
    }

    .image-preview {
      max-width: 100%;
      max-height: 200px;
      border-radius: 6px;
      border: 1px solid #ddd;
      object-fit: contain;
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

    .crop-name {
      color: #666;
      font-size: 0.9rem;
      margin-bottom: 1rem;
    }

    .confidence-score {
      display: flex;
      justify-content: space-between;
      margin: 0.75rem 0;
      color: #666;
    }

    .score {
      font-weight: 600;
      font-size: 1.1rem;
    }

    .score-high { color: #27ae60; }
    .score-medium { color: #f39c12; }
    .score-low { color: #e74c3c; }

    .symptoms-card p,
    .treatment-card p,
    .prevention-card p {
      color: #555;
      line-height: 1.7;
      margin: 0;
      white-space: pre-line;
    }

    /* Raw analysis fallback */
    .raw-analysis {
      color: #555;
      line-height: 1.7;
      white-space: pre-line;
      font-size: 0.95rem;
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
  previewUrl: string | null = null;
  isDragOver = false;
  isUploading = false;
  isAnalyzing = false;
  detectionResult: DiseaseDetectionResult | null = null;
  selectedLanguage = 'en';
  languages: Language[] = [];

  constructor(
    private diseaseDetectionService: DiseaseDetectionService,
    private toastr: ToastrService,
    private languageService: LanguageService
  ) { }

  ngOnInit(): void {
    this.languageService.getLanguages().subscribe({
      next: (response) => { this.languages = response.languages; },
      error: () => { /* handled in service with fallback */ }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.setFile(input.files[0]);
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
      this.setFile(event.dataTransfer.files[0]);
    }
  }

  private setFile(file: File): void {
    if (file.size > 10 * 1024 * 1024) {
      this.toastr.error('File size must be less than 10MB');
      return;
    }
    this.selectedFile = file;
    this.detectionResult = null;

    // Generate preview
    const reader = new FileReader();
    reader.onload = (e) => {
      this.previewUrl = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  uploadImage(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.isAnalyzing = true;
    this.detectionResult = null;

    this.diseaseDetectionService.detectDisease(this.selectedFile, this.selectedLanguage).subscribe({
      next: (result) => {
        this.isUploading = false;
        this.isAnalyzing = false;
        this.detectionResult = result;
        this.toastr.success('Disease detection completed');
        this.selectedFile = null;
        this.previewUrl = null;
      },
      error: (error) => {
        this.isUploading = false;
        this.isAnalyzing = false;
        this.toastr.error('Failed to analyze image. Please try again.');
        console.error('Disease detection error:', error);
      }
    });
  }

  getConfidenceLevel(score: number): string {
    if (score >= 0.8) return 'high';
    if (score >= 0.6) return 'medium';
    return 'low';
  }

  findNearbyKVK(): void {
    window.open('https://www.google.com/maps/search/Krishi+Vigyan+Kendra+near+me', '_blank');
  }
}
