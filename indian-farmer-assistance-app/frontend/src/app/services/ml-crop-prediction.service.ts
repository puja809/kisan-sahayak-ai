import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MLPredictionResponse {
  prediction: string;
  confidence: number;
  probabilities: { [key: string]: number };
  modelVersion: string;
}

export interface CropRecommendationRequest {
  latitude: number;
  longitude: number;
  nitrogen?: number;
  phosphorus?: number;
  potassium?: number;
}

export interface CropRotationRequest {
  previousCrop: string;
  latitude: number;
  longitude: number;
  season: string;
}

@Injectable({
  providedIn: 'root'
})
export class MLCropPredictionService {
  private apiUrl = '/api/crop/ml-predictions';

  constructor(private http: HttpClient) {}

  recommendCrop(request: CropRecommendationRequest): Observable<MLPredictionResponse> {
    const params = new URLSearchParams();
    params.append('latitude', request.latitude.toString());
    params.append('longitude', request.longitude.toString());
    params.append('nitrogen', (request.nitrogen || 100).toString());
    params.append('phosphorus', (request.phosphorus || 50).toString());
    params.append('potassium', (request.potassium || 50).toString());

    return this.http.post<MLPredictionResponse>(
      `${this.apiUrl}/recommend-crop?${params.toString()}`,
      {}
    );
  }

  recommendCropRotation(request: CropRotationRequest): Observable<MLPredictionResponse> {
    const params = new URLSearchParams();
    params.append('previousCrop', request.previousCrop);
    params.append('latitude', request.latitude.toString());
    params.append('longitude', request.longitude.toString());
    params.append('season', request.season);

    return this.http.post<MLPredictionResponse>(
      `${this.apiUrl}/recommend-rotation?${params.toString()}`,
      {}
    );
  }
}
