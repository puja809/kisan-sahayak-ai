import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

interface WeatherForecast {
  date: string;
  maxTemp: number;
  minTemp: number;
  humidity0830: number;
  humidity1730: number;
  rainfall: number;
  windSpeed: number;
  windDirection: string;
  cloudCoverage: number;
  sunriseTime: string;
  sunsetTime: string;
  moonriseTime: string;
  moonsetTime: string;
}

interface WeatherAlert {
  severity: string;
  description: string;
  timestamp: string;
}

interface AgrometAdvisory {
  cropStage: string;
  advisory: string;
  heatStressIndex: number;
  evapotranspiration: number;
}

@Component({
  selector: 'app-weather-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './weather-detail.component.html',
  styleUrls: ['./weather-detail.component.css'],})
export class WeatherDetailComponent implements OnInit {
  currentUser = this.authService.getCurrentUser();
  forecast: WeatherForecast[] = [];
  alerts: WeatherAlert[] = [];
  nowcastAlerts: string[] = [];
  agrometAdvisories: AgrometAdvisory[] = [];
  isOfflineData = false;
  cachedDataTime: Date | null = null;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadWeatherData();
  }

  private loadWeatherData(): void {
    if (!this.currentUser) return;

    this.http.get<any>(`/api/v1/weather/forecast/${this.currentUser.district}`).subscribe({
      next: (data) => {
        this.forecast = data.forecast || [];
        this.alerts = data.alerts || [];
        this.nowcastAlerts = data.nowcastAlerts || [];
        this.agrometAdvisories = data.agrometAdvisories || [];
        this.isOfflineData = false;
      },
      error: (error) => {
        console.error('Failed to load weather data:', error);
        this.loadCachedWeatherData();
      }
    });
  }

  private loadCachedWeatherData(): void {
    const cached = localStorage.getItem('weather_cache');
    if (cached) {
      const data = JSON.parse(cached);
      this.forecast = data.forecast || [];
      this.alerts = data.alerts || [];
      this.nowcastAlerts = data.nowcastAlerts || [];
      this.agrometAdvisories = data.agrometAdvisories || [];
      this.isOfflineData = true;
      this.cachedDataTime = new Date(data.timestamp);
    }
  }
}
