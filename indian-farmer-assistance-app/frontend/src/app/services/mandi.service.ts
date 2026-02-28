import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MandiPriceDto {
    id: number;
    commodityName: string;
    variety: string;
    mandiName: string;
    mandiCode: string;
    state: string;
    district: string;
    priceDate: string;
    modalPrice: number;
    minPrice: number;
    maxPrice: number;
    arrivalQuantityQuintals: number;
    unit: string;
    source: string;
    fetchedAt: string;
    distanceKm: number;
    isCached: boolean;
}

export interface FertilizerSupplierDto {
    state: string;
    district: string;
    documentId: string;
    slNo: number;
    noOfWholesalers: number;
    noOfRetailers: number;
    fertilizerType: string;
    supplierName: string;
    contactInfo: string;
    totalSuppliers: number;
}

@Injectable({
    providedIn: 'root'
})
export class MandiService {
    private apiUrl = '/api/v1/mandi';
    private fertilizerApiUrl = '/api/v1/fertilizer'; // Base URL for fertilizer endpoints

    constructor(private http: HttpClient) { }

    getCommodities(): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/commodities`);
    }

    getPrices(commodity: string): Observable<MandiPriceDto> {
        return this.http.get<MandiPriceDto>(`${this.apiUrl}/prices/${commodity}`);
    }

    getNearbyPrices(commodity: string, latitude: number, longitude: number, radiusKm: number = 50): Observable<MandiPriceDto> {
        let params = new HttpParams()
            .set('commodity', commodity)
            .set('latitude', latitude.toString())
            .set('longitude', longitude.toString())
            .set('radiusKm', radiusKm.toString());

        return this.http.get<MandiPriceDto>(`${this.apiUrl}/prices/nearby`, { params });
    }

    getStates(): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/states`);
    }

    getDistricts(state: string): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/states/${state}/districts`);
    }

    // --- Fertilizer Supplier Endpoints ---

    getFertilizerSuppliers(state?: string, district?: string, offset: number = 0, limit: number = 50): Observable<FertilizerSupplierDto[]> {
        let params = new HttpParams()
            .set('offset', offset.toString())
            .set('limit', limit.toString());
        if (state) params = params.set('state', state);
        if (district) params = params.set('district', district);

        return this.http.get<FertilizerSupplierDto[]>(`${this.fertilizerApiUrl}/suppliers`, { params });
    }

    getFertilizerSuppliersByType(state: string, district: string, fertilizerType: string): Observable<FertilizerSupplierDto[]> {
        let params = new HttpParams().set('state', state).set('district', district);
        return this.http.get<FertilizerSupplierDto[]>(`${this.fertilizerApiUrl}/suppliers/type/${encodeURIComponent(fertilizerType)}`, { params });
    }

    getFertilizerSuppliersByLocation(state: string, district: string): Observable<FertilizerSupplierDto[]> {
        let params = new HttpParams().set('state', state).set('district', district);
        return this.http.get<FertilizerSupplierDto[]>(`${this.fertilizerApiUrl}/suppliers/location`, { params });
    }

    getFertilizerWholesalers(state: string, district: string): Observable<FertilizerSupplierDto[]> {
        let params = new HttpParams().set('state', state).set('district', district);
        return this.http.get<FertilizerSupplierDto[]>(`${this.fertilizerApiUrl}/suppliers/wholesalers`, { params });
    }

    getFertilizerRetailers(state: string, district: string): Observable<FertilizerSupplierDto[]> {
        let params = new HttpParams().set('state', state).set('district', district);
        return this.http.get<FertilizerSupplierDto[]>(`${this.fertilizerApiUrl}/suppliers/retailers`, { params });
    }

    // --- Mandi Filter Endpoints ---

    getFilterStates(): Observable<string[]> {
        return this.http.get<string[]>('/api/mandi/filter/states');
    }

    getFilterDistricts(state: string): Observable<string[]> {
        let params = new HttpParams().set('state', state);
        return this.http.get<string[]>('/api/mandi/filter/districts', { params });
    }

    getFilterMarkets(state: string, district: string): Observable<string[]> {
        let params = new HttpParams().set('state', state).set('district', district);
        return this.http.get<string[]>('/api/mandi/filter/markets', { params });
    }

    getFilterCommodities(market?: string): Observable<string[]> {
        let params = new HttpParams();
        if (market) params = params.set('market', market);
        return this.http.get<string[]>('/api/mandi/filter/commodities', { params });
    }

    getFilterVarieties(commodity?: string): Observable<string[]> {
        let params = new HttpParams();
        if (commodity) params = params.set('commodity', commodity);
        return this.http.get<string[]>('/api/mandi/filter/varieties', { params });
    }

    getFilterGrades(commodity?: string, variety?: string): Observable<string[]> {
        let params = new HttpParams();
        if (commodity) params = params.set('commodity', commodity);
        if (variety) params = params.set('variety', variety);
        return this.http.get<string[]>('/api/mandi/filter/grades', { params });
    }

    searchMarketData(filters: any, offset: number = 0, limit: number = 20): Observable<MandiPriceDto[]> {
        let params = new HttpParams();
        if (filters.state) params = params.set('state', filters.state);
        if (filters.district) params = params.set('district', filters.district);
        if (filters.market) params = params.set('market', filters.market);
        if (filters.commodity) params = params.set('commodity', filters.commodity);
        if (filters.variety) params = params.set('variety', filters.variety);
        if (filters.grade) params = params.set('grade', filters.grade);

        params = params.set('offset', offset.toString());
        params = params.set('limit', limit.toString());

        return this.http.get<MandiPriceDto[]>('/api/mandi/filter/search', { params });
    }
}
