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
  template: `
    <div class="weather-container">
      <div class="header">
        <h1>Weather Forecast</h1>
        <p class="location" *ngIf="currentUser">{{ currentUser.district }}, {{ currentUser.state }}</p>
      </div>

      <!-- 7-Day Forecast -->
      <div class="card forecast-card">
        <h2>7-Day Forecast</h2>
        <div class="forecast-grid">
          <div *ngFor="let day of forecast" class="forecast-day">
            <p class="date">{{ day.date | date: 'EEE, MMM d' }}</p>
            <div class="temp-range">
              <span class="max">{{ day.maxTemp }}°</span>
              <span class="min">{{ day.minTemp }}°</span>
            </div>
            <p class="rainfall">💧 {{ day.rainfall }}mm</p>
            <p class="wind">💨 {{ day.windSpeed }} km/h</p>
            <p class="humidity">💧 {{ day.humidity0830 }}%</p>
          </div>
        </div>
      </div>

      <!-- Weather Alerts -->
      <div class="card alerts-card" *ngIf="alerts.length > 0">
        <h2>Weather Alerts</h2>
        <div class="alerts-list">
          <div *ngFor="let alert of alerts" class="alert-item" [class]="'severity-' + alert.severity.toLowerCase()">
            <p><strong>{{ alert.severity }}</strong></p>
            <p>{{ alert.description }}</p>
            <p class="timestamp">{{ alert.timestamp | date: 'short' }}</p>
          </div>
        </div>
      </div>

      <!-- Nowcast Alerts -->
      <div class="card nowcast-card" *ngIf="nowcastAlerts.length > 0">
        <h2>Nowcast Alerts (0-3 hours)</h2>
        <div class="nowcast-list">
          <div *ngFor="let alert of nowcastAlerts" class="nowcast-item">
            <p>{{ alert }}</p>
          </div>
        </div>
      </div>

      <!-- Agromet Advisories -->
      <div class="card advisory-card" *ngIf="agrometAdvisories.length > 0">
        <h2>Agromet Advisories</h2>
        <div class="advisory-list">
          <div *ngFor="let advisory of agrometAdvisories" class="advisory-item">
            <h3>{{ advisory.cropStage }}</h3>
            <p>{{ advisory.advisory }}</p>
            <p>Heat Stress Index: {{ advisory.heatStressIndex }}</p>
            <p>ET₀: {{ advisory.evapotranspiration }} mm</p>
          </div>
        </div>
      </div>

      <!-- Offline Cache Notice -->
      <div *ngIf="isOfflineData" class="offline-notice">
        <p>📡 Displaying cached data from {{ cachedDataTime | date: 'short' }}</p>
      </div>
    </div>
  `,
  styles: [`
    .weather-container {
      padding: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    .header {
      margin-bottom: 2rem;
    }

    .header h1 {
      font-size: 2rem;
      color: #333;
      margin-bottom: 0.5rem;
    }

    .location {
      color: #666;
      font-size: 1.1rem;
    }

    .card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      margin-bottom: 1.5rem;
    }

    .card h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .forecast-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 1rem;
    }

    .forecast-day {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      text-align: center;
    }

    .forecast-day .date {
      font-weight: 600;
      color: #333;
      margin-bottom: 0.5rem;
    }

    .temp-range {
      display: flex;
      justify-content: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }

    .temp-range .max {
      color: #e74c3c;
      font-weight: 600;
    }

    .temp-range .min {
      color: #3498db;
      font-weight: 600;
    }

    .forecast-day p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .alerts-list,
    .nowcast-list,
    .advisory-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .alert-item,
    .nowcast-item,
    .advisory-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .alert-item.severity-high {
      border-left-color: #e74c3c;
      background: #fadbd8;
    }

    .alert-item.severity-medium {
      border-left-color: #f39c12;
      background: #fef5e7;
    }

    .alert-item.severity-low {
      border-left-color: #3498db;
      background: #ebf5fb;
    }

    .alert-item p,
    .advisory-item p {
      margin: 0.25rem 0;
      color: #666;
    }

    .alert-item strong {
      color: #333;
    }

    .timestamp {
      font-size: 0.85rem;
      color: #999;
    }

    .advisory-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .offline-notice {
      padding: 1rem;
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      color: #856404;
      text-align: center;
    }

    @media (max-width: 768px) {
      .forecast-grid {
        grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
      }
    }
  `]
})
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
