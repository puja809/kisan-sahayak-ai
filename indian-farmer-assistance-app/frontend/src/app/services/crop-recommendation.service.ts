import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CropPrediction {
  prediction: string;
  confidence: number;
  probabilities: { [key: string]: number };
  modelVersion: string;
}

export interface FertilizerRecommendation {
  N_dosage: number;
  P_dosage: number;
  K_dosage: number;
  total_dosage: number;
  modelVersion: string;
}

export interface SoilData {
  soil_type: { texture_class: string; fao_class: string };
  physical_properties: {
    sand_pct: number;
    silt_pct: number;
    clay_pct: number;
    bulk_density_g_cm3: number;
  };
  chemical_properties: {
    ph_h2o: number;
    organic_matter_pct: number;
    nitrogen_g_kg: number;
    cec_cmol_kg: number;
  };
  water_metrics: {
    capacity_field_vol_pct: number;
    capacity_wilt_vol_pct: number;
  };
}

export interface WeatherData {
  current: { temp_c: number; humidity: number; precip_mm: number };
  forecast: {
    forecastday: Array<{
      day: { avgtemp_c: number; avghumidity: number; totalprecip_mm: number };
    }>;
  };
}

export interface DashboardResponse {
  cropRecommendation: CropPrediction;
  cropRotation: CropPrediction;
  fertilizerRecommendation: FertilizerRecommendation;
  soilData: SoilData;
  weatherData: WeatherData;
  location: string;
}

@Injectable({
  providedIn: 'root'
})
export class CropRecommendationService {
  private apiUrl = '/api/v1/crops/dashboard';

  constructor(private http: HttpClient) { }

  getDashboardRecommendations(
    latitude: number,
    longitude: number,
    season?: string,
    previousCrop?: string
  ): Observable<DashboardResponse> {
    let url = `${this.apiUrl}/recommendations?latitude=${latitude}&longitude=${longitude}`;
    if (season) url += `&season=${season}`;
    if (previousCrop) url += `&previousCrop=${previousCrop}`;
    return this.http.get<DashboardResponse>(url);
  }
}
