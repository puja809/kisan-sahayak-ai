import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface State {
  id: number;
  stateName: string;
  isActive: boolean;
}

export interface District {
  id: number;
  districtName: string;
  stateId: number;
  stateName: string;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class StateDistrictService {
  private apiUrl = `${environment.services.mandi}/api/v1/mandi/states-districts`;

  constructor(private http: HttpClient) {}

  getAllStates(): Observable<State[]> {
    return this.http.get<State[]>(`${this.apiUrl}/states`);
  }

  getDistrictsByState(stateName: string): Observable<District[]> {
    return this.http.get<District[]>(`${this.apiUrl}/states/${stateName}/districts`);
  }
}
