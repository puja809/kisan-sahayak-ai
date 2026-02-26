import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CurrentWeatherDto {
    district: string;
    state?: string;
    observationTime: string;
    cloudCoverage: number;
    rainfall24HoursMm: number;
    windSpeedKmph: number;
    windDirection: string;
    temperatureCelsius: number;
    humidity: number;
    pressureHpa: number;
    visibilityKm: number;
    weatherDescription: string;
}

export interface ForecastDayDto {
    date: string;
    maxTempCelsius: number;
    minTempCelsius: number;
    humidity0830: number;
    humidity1730: number;
    rainfallMm: number;
    windSpeedKmph: number;
    windDirection: string;
    cloudCoverage: number;
    sunriseTime: string;
    sunsetTime: string;
}

export interface SevenDayForecastDto {
    district: string;
    state?: string;
    forecastDays: ForecastDayDto[];
}

export interface AdvisoryDto {
    type: string;
    title: string;
    description: string;
    priority: string;
    recommendations: string[];
}

export interface AgrometAdvisoryDto {
    district: string;
    state?: string;
    cropStage: string;
    majorCrop: string;
    advisories: AdvisoryDto[];
    generalAdvice: string;
}

@Injectable({
    providedIn: 'root'
})
export class WeatherService {
    private apiUrl = '/api/v1/weather';

    constructor(private http: HttpClient) { }

    getCurrentWeather(district: string, state?: string): Observable<CurrentWeatherDto> {
        let params = new HttpParams();
        if (state) params = params.set('state', state);
        return this.http.get<CurrentWeatherDto>(`${this.apiUrl}/current/${encodeURIComponent(district)}`, { params });
    }

    getSevenDayForecast(district: string, state?: string): Observable<SevenDayForecastDto> {
        let params = new HttpParams();
        if (state) params = params.set('state', state);
        return this.http.get<SevenDayForecastDto>(`${this.apiUrl}/forecast/${encodeURIComponent(district)}`, { params });
    }

    getAgrometAdvisories(district: string, state?: string): Observable<AgrometAdvisoryDto> {
        let params = new HttpParams();
        if (state) params = params.set('state', state);
        return this.http.get<AgrometAdvisoryDto>(`${this.apiUrl}/agromet/${encodeURIComponent(district)}`, { params });
    }
}
