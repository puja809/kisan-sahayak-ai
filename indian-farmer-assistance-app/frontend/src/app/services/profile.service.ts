import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum CropStatus {
    SOWN = 'SOWN',
    GROWING = 'GROWING',
    HARVESTED = 'HARVESTED'
}

export enum Season {
    KHARIF = 'KHARIF',
    RABI = 'RABI',
    SUMMER = 'SUMMER'
}

export interface CropRequest {
    cropName: string;
    cropVariety?: string;
    sowingDate: string; // LocalDate as string
    expectedHarvestDate?: string;
    areaAcres: number;
    season: Season;
    status?: CropStatus;
    seedCost?: number;
    fertilizerCost?: number;
    pesticideCost?: number;
    laborCost?: number;
    otherCost?: number;
    totalYieldQuintals?: number;
    qualityGrade?: string;
    sellingPricePerQuintal?: number;
    mandiName?: string;
    totalRevenue?: number;
    actualHarvestDate?: string;
    notes?: string;
}

export interface CropResponse {
    id: number;
    userId: number;
    cropName: string;
    cropVariety?: string;
    sowingDate: string;
    expectedHarvestDate?: string;
    actualHarvestDate?: string;
    areaAcres: number;
    season: string;
    status: string;
    seedCost?: number;
    fertilizerCost?: number;
    pesticideCost?: number;
    laborCost?: number;
    otherCost?: number;
    totalInputCost?: number;
    totalYieldQuintals?: number;
    qualityGrade?: string;
    sellingPricePerQuintal?: number;
    mandiName?: string;
    totalRevenue?: number;
    notes?: string;
    createdAt: string;
    updatedAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class ProfileService {
    private apiUrl = '/api/v1/profile';

    constructor(private http: HttpClient) { }

    getCrops(): Observable<CropResponse[]> {
        return this.http.get<CropResponse[]>(`${this.apiUrl}/crops`);
    }

    addCrop(request: CropRequest): Observable<CropResponse> {
        return this.http.post<CropResponse>(`${this.apiUrl}/crops`, request);
    }

    getCrop(cropId: number): Observable<CropResponse> {
        return this.http.get<CropResponse>(`${this.apiUrl}/crops/${cropId}`);
    }

    updateCrop(cropId: number, request: CropRequest): Observable<CropResponse> {
        return this.http.put<CropResponse>(`${this.apiUrl}/crops/${cropId}`, request);
    }

    deleteCrop(cropId: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/crops/${cropId}`);
    }
}
