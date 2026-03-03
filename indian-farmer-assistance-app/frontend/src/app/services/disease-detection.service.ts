import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DiseaseDetectionResult {
  crop: string;
  disease: string;
  symptoms: string;
  treatment: string;
  prevention: string;
  confidence: number;
  modelVersion?: string;
  raw_analysis?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DiseaseDetectionService {
  private apiUrl = '/api/ml/disease-detect';

  constructor(private http: HttpClient) { }

  detectDisease(imageFile: File, language: string = 'en'): Observable<DiseaseDetectionResult> {
    const formData = new FormData();
    formData.append('image', imageFile);

    // Language and session_id are sent as query params (FastAPI Query params)
    const params = new HttpParams()
      .set('language', language)
      .set('session_id', this.getSessionId());

    return this.http.post<DiseaseDetectionResult>(this.apiUrl, formData, { params });
  }

  private getSessionId(): string {
    let sessionId = sessionStorage.getItem('kisan_session_id');
    if (!sessionId) {
      sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
      sessionStorage.setItem('kisan_session_id', sessionId);
    }
    return sessionId;
  }
}
