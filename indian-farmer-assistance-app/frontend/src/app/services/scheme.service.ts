import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Scheme {
  id: number;
  name: string;
  description: string;
  eligibility: string;
  benefits: string;
  applicationDeadline: string;
  state?: string;
  district?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SchemeService {
  private apiUrl = '/api/v1/schemes';

  constructor(private http: HttpClient) { }

  getAllSchemes(): Observable<Scheme[]> {
    return this.http.get<Scheme[]>(`${this.apiUrl}`);
  }

  getSchemesByState(state: string): Observable<Scheme[]> {
    return this.http.get<Scheme[]>(`${this.apiUrl}/state/${state}`);
  }

  getSchemesByStateAndDistrict(state: string, district: string): Observable<Scheme[]> {
    return this.http.get<Scheme[]>(`${this.apiUrl}/state/${state}/district/${district}`);
  }
}
