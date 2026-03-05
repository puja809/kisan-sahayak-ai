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
  templateUrl: './disease-detection.component.html',
  styleUrls: ['./disease-detection.component.css'],})
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
