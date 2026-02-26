import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { WeatherService, CurrentWeatherDto, SevenDayForecastDto, AgrometAdvisoryDto } from '../../services/weather.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-weather',
  standalone: true,
  imports: [CommonModule],
  providers: [DatePipe],
  template: `
    <div class="weather-page">
      <div class="header">
        <h2>Local Weather Forecast</h2>
        <p>Current conditions and 7-day forecasts for your farm planning.</p>
      </div>

      <div class="loader" *ngIf="isLoading">
        <div class="spinner"></div>
        <p>Loading weather data for {{ currentDistrict }}...</p>
      </div>

      <div class="error-banner" *ngIf="errorMessage">
        {{ errorMessage }}
      </div>

      <ng-container *ngIf="!isLoading && !errorMessage">
        <!-- Current Weather -->
        <div class="current-weather" *ngIf="currentWeather">
          <div class="weather-main">
            <div class="location-info">
              <h3>{{ currentWeather.district }}, {{ currentWeather.state }}</h3>
              <p class="time">{{ currentWeather.observationTime | date:'mediumTime' }}</p>
            </div>
            <div class="temp-display">
              <span class="temp">{{ currentWeather.temperatureCelsius | number:'1.0-0' }}°C</span>
              <span class="desc">{{ currentWeather.weatherDescription }}</span>
            </div>
          </div>
          
          <div class="weather-details">
            <div class="detail-item">
              <span class="icon">💧</span>
              <span class="label">Humidity</span>
              <span class="value">{{ currentWeather.humidity }}%</span>
            </div>
            <div class="detail-item">
              <span class="icon">💨</span>
              <span class="label">Wind</span>
              <span class="value">{{ currentWeather.windSpeedKmph }} km/h ({{ currentWeather.windDirection }})</span>
            </div>
            <div class="detail-item">
              <span class="icon">🌧️</span>
              <span class="label">Rain (24h)</span>
              <span class="value">{{ currentWeather.rainfall24HoursMm }} mm</span>
            </div>
            <div class="detail-item">
              <span class="icon">☁️</span>
              <span class="label">Cloud Cover</span>
              <span class="value">{{ currentWeather.cloudCoverage }}%</span>
            </div>
          </div>
        </div>

        <!-- 7-Day Forecast -->
        <h3 class="section-title" *ngIf="forecast?.forecastDays?.length">7-Day Forecast</h3>
        <div class="forecast-grid" *ngIf="forecast?.forecastDays?.length">
          <div class="forecast-card" *ngFor="let day of forecast?.forecastDays">
            <h4>{{ day.date | date:'EEE, MMM d' }}</h4>
            <div class="temps">
              <span class="max">{{ day.maxTempCelsius | number:'1.0-0' }}°</span>
              <span class="min">{{ day.minTempCelsius | number:'1.0-0' }}°</span>
            </div>
            <div class="day-details">
              <span>💧 {{ day.rainfallMm > 0 ? day.rainfallMm + 'mm' : 'Dry' }}</span>
              <span>💨 {{ day.windSpeedKmph }} km/h</span>
            </div>
          </div>
        </div>

        <!-- Agromet Advisories -->
        <div class="advisories-section" *ngIf="advisories?.advisories?.length">
          <h3 class="section-title">Agro-Meteorological Advisories</h3>
          <p class="general-advice" *ngIf="advisories?.generalAdvice">{{ advisories?.generalAdvice }}</p>
          
          <div class="advisory-cards">
            <div class="advisory-card" *ngFor="let advisory of advisories?.advisories" [ngClass]="advisory.priority.toLowerCase()">
              <div class="card-header">
                <span class="type">{{ advisory.type }}</span>
                <span class="priority" *ngIf="advisory.priority !== 'NORMAL'">{{ advisory.priority }}</span>
              </div>
              <h4>{{ advisory.title }}</h4>
              <p>{{ advisory.description }}</p>
              
              <ul class="recommendations" *ngIf="advisory.recommendations.length">
                <li *ngFor="let rec of advisory.recommendations">👉 {{ rec }}</li>
              </ul>
            </div>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .weather-page {
      padding: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
    }
    .header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .header h2 {
      color: #2E7D32;
      font-size: 2rem;
      margin-bottom: 0.5rem;
    }
    .current-weather {
      background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%);
      color: white;
      border-radius: 16px;
      padding: 2rem;
      margin-bottom: 2.5rem;
      box-shadow: 0 8px 16px rgba(30, 136, 229, 0.2);
    }
    .weather-main {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid rgba(255,255,255,0.2);
      padding-bottom: 1.5rem;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .location-info h3 {
      font-size: 1.8rem;
      margin: 0 0 0.25rem 0;
    }
    .time {
      opacity: 0.8;
      margin: 0;
    }
    .temp-display {
      text-align: right;
    }
    .temp {
      font-size: 3.5rem;
      font-weight: 700;
      line-height: 1;
      display: block;
    }
    .desc {
      font-size: 1.2rem;
      text-transform: capitalize;
      opacity: 0.9;
    }
    .weather-details {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
    }
    .detail-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      background: rgba(255,255,255,0.1);
      padding: 1rem;
      border-radius: 12px;
    }
    .detail-item .icon {
      font-size: 1.5rem;
      margin-bottom: 0.5rem;
    }
    .detail-item .label {
      font-size: 0.85rem;
      opacity: 0.8;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .detail-item .value {
      font-size: 1.1rem;
      font-weight: 600;
      margin-top: 0.25rem;
    }
    
    .section-title {
      color: #2E7D32;
      font-size: 1.5rem;
      margin-bottom: 1rem;
      border-bottom: 2px solid #E8F5E9;
      padding-bottom: 0.5rem;
    }
    
    .forecast-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
      gap: 1rem;
      margin-bottom: 3rem;
    }
    .forecast-card {
      background: white;
      border-radius: 12px;
      padding: 1.25rem;
      text-align: center;
      box-shadow: 0 4px 6px rgba(0,0,0,0.05);
      border: 1px solid #f0f0f0;
    }
    .forecast-card h4 {
      margin: 0 0 1rem 0;
      color: #424242;
      font-size: 1rem;
    }
    .temps {
      margin-bottom: 1rem;
    }
    .temps .max {
      font-size: 1.5rem;
      font-weight: 600;
      color: #D32F2F;
      margin-right: 0.5rem;
    }
    .temps .min {
      font-size: 1.2rem;
      color: #1976D2;
    }
    .day-details {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      font-size: 0.85rem;
      color: #616161;
    }
    
    .advisories-section {
      margin-bottom: 2rem;
    }
    .general-advice {
      background: #E8F5E9;
      padding: 1rem;
      border-radius: 8px;
      color: #1B5E20;
      margin-bottom: 1.5rem;
      border-left: 4px solid #4CAF50;
    }
    .advisory-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1.5rem;
    }
    .advisory-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 4px 6px rgba(0,0,0,0.05);
      border-top: 4px solid #2196F3; /* Default blue */
    }
    .advisory-card.high {
      border-top-color: #D32F2F; /* Red */
    }
    .advisory-card.medium {
      border-top-color: #F57C00; /* Orange */
    }
    .card-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.75rem;
      font-size: 0.85rem;
      text-transform: uppercase;
      font-weight: 600;
      color: #757575;
    }
    .priority {
      color: white;
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
    }
    .high .priority { background: #D32F2F; }
    .medium .priority { background: #F57C00; }
    
    .advisory-card h4 {
      margin: 0 0 0.5rem 0;
      font-size: 1.1rem;
      color: #333;
    }
    .advisory-card p {
      color: #666;
      font-size: 0.95rem;
      line-height: 1.5;
      margin-bottom: 1rem;
    }
    .recommendations {
      list-style-type: none;
      padding: 0;
      margin: 0;
    }
    .recommendations li {
      padding: 0.5rem 0;
      border-top: 1px solid #eee;
      font-size: 0.9rem;
      color: #424242;
    }
    
    .loader {
      text-align: center;
      padding: 3rem;
    }
    .spinner {
      border: 4px solid #f3f3f3;
      border-top: 4px solid #2E7D32;
      border-radius: 50%;
      width: 40px;
      height: 40px;
      animation: spin 1s linear infinite;
      margin: 0 auto 1rem;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    .error-banner {
      background: #FFEBEE;
      color: #D32F2F;
      padding: 1rem;
      border-radius: 8px;
      text-align: center;
      margin-bottom: 2rem;
    }
  `]
})
export class WeatherComponent implements OnInit {
  isLoading = true;
  errorMessage = '';

  // State
  currentDistrict = 'Pune'; // Default
  currentWeather?: CurrentWeatherDto;
  forecast?: SevenDayForecastDto;
  advisories?: AgrometAdvisoryDto;

  constructor(
    private weatherService: WeatherService,
    private http: HttpClient
  ) { }

  ngOnInit() {
    this.detectLocationAndFetch();
  }

  detectLocationAndFetch() {
    this.isLoading = true;
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          try {
            // Very simple reverse geocode using public BigDataCloud API for District name
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            const response: any = await firstValueFrom(
              this.http.get(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=en`)
            );

            // Attempt to extract district/city
            if (response && (response.city || response.locality)) {
              this.currentDistrict = response.city || response.locality;
            }
            this.fetchAllWeatherData();
          } catch (e) {
            console.error('Reverse geocode failed, using default Pune', e);
            this.fetchAllWeatherData();
          }
        },
        () => {
          console.warn('Geolocation denied or failed, using default Pune');
          this.fetchAllWeatherData();
        }
      );
    } else {
      this.fetchAllWeatherData();
    }
  }

  fetchAllWeatherData() {
    this.isLoading = true;
    this.errorMessage = '';

    // Fetch all three endpoints
    let pending = 3;
    const checkDone = () => {
      pending--;
      if (pending === 0) this.isLoading = false;
    };

    this.weatherService.getCurrentWeather(this.currentDistrict).subscribe({
      next: (data) => { this.currentWeather = data; checkDone(); },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to load weather data from backend.';
        checkDone();
      }
    });

    this.weatherService.getSevenDayForecast(this.currentDistrict).subscribe({
      next: (data) => { this.forecast = data; checkDone(); },
      error: () => checkDone()
    });

    this.weatherService.getAgrometAdvisories(this.currentDistrict).subscribe({
      next: (data) => { this.advisories = data; checkDone(); },
      error: () => checkDone()
    });
  }
}
