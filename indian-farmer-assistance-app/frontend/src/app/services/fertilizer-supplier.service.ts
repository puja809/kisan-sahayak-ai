import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FertilizerSupplier {
  state: string;
  district: string;
  documentId: string;
  slNo: number;
  noOfWholesalers: number;
  noOfRetailers: number;
  fertilizerType: string;
  supplierName: string;
  contactInfo: string;
}

@Injectable({
  providedIn: 'root'
})
export class FertilizerSupplierService {
  
  private apiUrl = '/api/v1/fertilizer/suppliers';

  constructor(private http: HttpClient) { }

  /**
   * Get fertilizer suppliers by location.
   * 
   * @param state State name
   * @param district District name
   * @returns Observable of fertilizer suppliers
   */
  getSuppliersByLocation(state: string, district: string): Observable<FertilizerSupplier[]> {
    const params = new HttpParams()
      .set('state', state)
      .set('district', district);
    
    return this.http.get<FertilizerSupplier[]>(`${this.apiUrl}/location`, { params });
  }

  /**
   * Get fertilizer suppliers by state.
   * 
   * @param state State name
   * @returns Observable of fertilizer suppliers
   */
  getSuppliersByState(state: string): Observable<FertilizerSupplier[]> {
    return this.http.get<FertilizerSupplier[]>(`${this.apiUrl}/state/${state}`);
  }

  /**
   * Get fertilizer suppliers sorted by wholesalers.
   * 
   * @param state State name
   * @param district District name
   * @returns Observable of fertilizer suppliers
   */
  getSuppliersByWholesalers(state: string, district: string): Observable<FertilizerSupplier[]> {
    const params = new HttpParams()
      .set('state', state)
      .set('district', district);
    
    return this.http.get<FertilizerSupplier[]>(`${this.apiUrl}/wholesalers`, { params });
  }

  /**
   * Get fertilizer suppliers sorted by retailers.
   * 
   * @param state State name
   * @param district District name
   * @returns Observable of fertilizer suppliers
   */
  getSuppliersByRetailers(state: string, district: string): Observable<FertilizerSupplier[]> {
    const params = new HttpParams()
      .set('state', state)
      .set('district', district);
    
    return this.http.get<FertilizerSupplier[]>(`${this.apiUrl}/retailers`, { params });
  }

  /**
   * Get fertilizer suppliers by type.
   * 
   * @param state State name
   * @param district District name
   * @param fertilizerType Fertilizer type
   * @returns Observable of fertilizer suppliers
   */
  getSuppliersByType(state: string, district: string, fertilizerType: string): Observable<FertilizerSupplier[]> {
    const params = new HttpParams()
      .set('state', state)
      .set('district', district);
    
    return this.http.get<FertilizerSupplier[]>(`${this.apiUrl}/type/${fertilizerType}`, { params });
  }

  /**
   * Get all states.
   * 
   * @returns Observable of state names
   */
  getStates(): Observable<string[]> {
    return this.http.get<string[]>('/api/v1/location/states');
  }

  /**
   * Get districts for a state.
   * 
   * @param state State name
   * @returns Observable of district names
   */
  getDistricts(state: string): Observable<string[]> {
    return this.http.get<string[]>(`/api/v1/location/districts/${state}`);
  }
}
