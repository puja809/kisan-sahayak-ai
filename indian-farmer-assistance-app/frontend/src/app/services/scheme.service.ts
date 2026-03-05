import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { map } from 'rxjs/operators';

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
    return this.http.get<any>(`${this.apiUrl}?size=100`).pipe(
      map(res => this.mapSchemes(res.content || []))
    );
  }

  getSchemesByState(state: string): Observable<Scheme[]> {
    return this.http.get<any>(`${this.apiUrl}?state=${state}&size=100`).pipe(
      map(res => this.mapSchemes(res.content || []))
    );
  }

  getSchemesByStateAndDistrict(state: string, district: string): Observable<Scheme[]> {
    return this.http.get<any>(`${this.apiUrl}?state=${state}&size=100`).pipe(
      map(res => this.mapSchemes(res.content || []))
    );
  }

  private mapSchemes(data: any[]): Scheme[] {
    return data.map(item => ({
      id: item.id,
      name: item.schemeName || 'Unknown Scheme',
      description: item.schemeDetails || 'No details available',
      eligibility: item.responsibleMinistry || '', // mapping as fallback
      benefits: item.implementingOffice || '',
      applicationDeadline: item.websiteLink ? `Website: ${item.websiteLink}` : '',
      state: item.centerStateName
    }));
  }
}
