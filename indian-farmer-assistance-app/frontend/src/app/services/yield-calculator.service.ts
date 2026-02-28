import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface YieldCalculationRequest {
  commodity: string;
  farmSizeHectares: number;
  investmentAmount: number;
}

export interface YieldCalculationResponse {
  commodity: string;
  farmSizeHectares: number;
  investmentAmount: number;
  minPricePerKg: number;
  avgPricePerKg: number;
  maxPricePerKg: number;
  baseYieldPerHectare: number;
  estimatedMinYield: number;
  estimatedExpectedYield: number;
  estimatedMaxYield: number;
  estimatedMinRevenue: number;
  estimatedExpectedRevenue: number;
  estimatedMaxRevenue: number;
  profitMarginPercent: number;
  message: string;
  success: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class YieldCalculatorService {

  private yieldApiUrl = `${environment.apiUrl}/crops/yield/calculate`;
  private mandiApiUrl = `${environment.apiUrl}/mandi/filter/commodities`;

  constructor(private http: HttpClient) { }

  /**
   * Calculate yield based on commodity, farm size, and investment amount
   * @param request Yield calculation request
   * @returns Observable of yield calculation response
   */
  calculateYield(request: YieldCalculationRequest): Observable<YieldCalculationResponse> {
    return this.http.post<YieldCalculationResponse>(this.yieldApiUrl, request);
  }

  /**
   * Get all available commodities from mandi-service
   * @returns Observable of commodity list
   */
  getCommodities(): Observable<string[]> {
    return this.http.get<string[]>(this.mandiApiUrl);
  }
}
