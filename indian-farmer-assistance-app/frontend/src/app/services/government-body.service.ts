import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface GovernmentBody {
  id: number;
  state: string;
  district: string;
  officeName: string;
  officerName: string;
  designation: string;
  phone: string;
  email: string;
  address: string;
}

interface BackendGovernmentBody {
  id: number;
  state: string;
  district: string;
  districtOfficer: string;
  districtPhone: string;
  email: string;
  kvkPhone: string;
  sampleVillage: string;
  bodyType: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class GovernmentBodyService {
  private apiUrl = '/api/v1/location/government-bodies';

  constructor(private http: HttpClient) { }

  private mapBackendToFrontend(backend: BackendGovernmentBody): GovernmentBody {
    return {
      id: backend.id,
      state: backend.state,
      district: backend.district,
      officeName: backend.district + ' Agricultural Office',
      officerName: backend.districtOfficer || 'N/A',
      designation: 'District Officer',
      phone: backend.districtPhone || backend.kvkPhone || 'N/A',
      email: backend.email || 'N/A',
      address: backend.sampleVillage || backend.district + ', ' + backend.state
    };
  }

  searchByLocation(state: string, district?: string): Observable<GovernmentBody[]> {
    const params = district ? `?state=${state}&district=${district}` : `?state=${state}`;
    return this.http.get<BackendGovernmentBody[]>(`${this.apiUrl}/search${params}`).pipe(
      map(bodies => bodies.map(b => this.mapBackendToFrontend(b)))
    );
  }

  getByState(state: string): Observable<GovernmentBody[]> {
    return this.http.get<BackendGovernmentBody[]>(`${this.apiUrl}/state/${state}`).pipe(
      map(bodies => bodies.map(b => this.mapBackendToFrontend(b)))
    );
  }

  getByDistrict(district: string): Observable<GovernmentBody[]> {
    return this.http.get<BackendGovernmentBody[]>(`${this.apiUrl}/district/${district}`).pipe(
      map(bodies => bodies.map(b => this.mapBackendToFrontend(b)))
    );
  }

  getByStateAndDistrict(state: string, district: string): Observable<GovernmentBody[]> {
    return this.http.get<BackendGovernmentBody[]>(`${this.apiUrl}/state/${state}/district/${district}`).pipe(
      map(bodies => bodies.map(b => this.mapBackendToFrontend(b)))
    );
  }
}
