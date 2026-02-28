import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { GeolocationService } from '../../services/geolocation.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="home-container">
      <section class="hero">
        <h1>Welcome to Kisan Sahayak AI</h1>
        <p>Your comprehensive platform for agricultural success</p>
      </section>
      
      <!-- Weather Advisory Section -->
      <section class="weather-advisory" *ngIf="weatherData" (click)="goToWeather()">
        <div class="weather-header">
          <span class="weather-icon">🌤️</span>
          <h2>Weather Advisory</h2>
        </div>
        <div class="weather-location">
          <span class="location-icon">📍</span>
          {{ weatherData.location }}
        </div>
        <div class="weather-description">
          {{ weatherData.description }}
        </div>
        <div class="weather-forecast" *ngIf="weatherData.forecast">
          <p><strong>{{ weatherData.forecast.day }}:</strong> {{ weatherData.forecast.condition }}</p>
          <p class="temperature">{{ weatherData.forecast.temp }}</p>
        </div>
        <a href="javascript:void(0)" class="read-more">Read More...</a>
      </section>
      
      <section class="features">
        <div class="feature-card" routerLink="/weather">
          <div class="feature-icon">🌤️</div>
          <h3>Weather Forecast</h3>
          <p>7-day weather forecasts, alerts, and agromet advisories for your district</p>
        </div>
        
        <div class="feature-card" routerLink="/crops">
          <div class="feature-icon">🌾</div>
          <h3>Crop Recommendations</h3>
          <p>AI-powered crop suggestions based on your location and soil conditions</p>
        </div>
        
        <div class="feature-card" routerLink="/schemes">
          <div class="feature-icon">📋</div>
          <h3>Government Schemes</h3>
          <p>Discover and apply for central and state government schemes</p>
        </div>
        
        <div class="feature-card" routerLink="/mandi">
          <div class="feature-icon">💰</div>
          <h3>Mandi Prices</h3>
          <p>Real-time commodity prices from agricultural markets near you</p>
        </div>

        <div class="feature-card" routerLink="/voice">
          <div class="feature-icon">🎤</div>
          <h3>Voice Assistant</h3>
          <p>Talk to our AI assistant in your preferred language for instant help</p>
        </div>

        <div class="feature-card" routerLink="/disease">
          <div class="feature-icon">🦠</div>
          <h3>Disease Detection</h3>
          <p>Upload crop photos to identify diseases and get treatment suggestions</p>
        </div>

        <div class="feature-card" routerLink="/iot">
          <div class="feature-icon">📡</div>
          <h3>IoT Dashboard</h3>
          <p>Monitor your smart farm sensors — soil moisture, temperature, and more</p>
        </div>

        <div class="feature-card" routerLink="/location">
          <div class="feature-icon">📍</div>
          <h3>Location Services</h3>
          <p>Set your location to get localized weather, mandi, and scheme info</p>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .home-container {
      padding: 2rem 0;
    }
    
    .hero {
      text-align: center;
      padding: 3rem 1rem;
      background: linear-gradient(135deg, #2E7D32 0%, #4CAF50 100%);
      color: white;
      border-radius: 12px;
      margin-bottom: 2rem;
      
      h1 {
        font-size: 2rem;
        margin-bottom: 0.5rem;
      }
      
      p {
        font-size: 1.2rem;
        opacity: 0.9;
      }
    }

    .weather-advisory {
      background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
      color: white;
      border-radius: 12px;
      padding: 2rem;
      margin-bottom: 2rem;
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
      }

      .weather-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 1rem;

        .weather-icon {
          font-size: 2rem;
          margin-right: 1rem;
        }

        h2 {
          flex: 1;
          margin: 0;
          font-size: 1.5rem;
        }

        .change-location-btn {
          background: rgba(255, 255, 255, 0.2);
          border: 1px solid rgba(255, 255, 255, 0.4);
          color: white;
          padding: 0.5rem 1rem;
          border-radius: 6px;
          cursor: pointer;
          font-size: 0.85rem;
          transition: background 0.2s;

          &:hover {
            background: rgba(255, 255, 255, 0.3);
          }
        }
      }

      .weather-location {
        display: flex;
        align-items: center;
        margin-bottom: 1rem;
        font-size: 0.95rem;
        opacity: 0.9;

        .location-icon {
          margin-right: 0.5rem;
        }
      }

      .weather-description {
        margin-bottom: 1rem;
        line-height: 1.6;
        font-size: 0.95rem;
      }

      .weather-forecast {
        background: rgba(255, 255, 255, 0.1);
        padding: 1rem;
        border-radius: 8px;
        margin-bottom: 1rem;

        p {
          margin: 0.5rem 0;
          font-size: 0.9rem;

          &.temperature {
            font-size: 1.1rem;
            font-weight: 600;
          }
        }
      }

      .read-more {
        color: rgba(255, 255, 255, 0.8);
        text-decoration: none;
        font-size: 0.9rem;
        transition: color 0.2s;

        &:hover {
          color: white;
          text-decoration: underline;
        }
      }
    }
    
    .features {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
    }
    
    .feature-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
      }
      
      .feature-icon {
        font-size: 3rem;
        margin-bottom: 1rem;
      }
      
      h3 {
        color: #2E7D32;
        margin-bottom: 0.5rem;
      }
      
      p {
        color: #757575;
        font-size: 0.9rem;
      }
    }
  `]
})
export class HomeComponent implements OnInit {
  weatherData: any = null;

  constructor(
    private geolocationService: GeolocationService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadWeatherData();
  }

  loadWeatherData() {
    // Mock weather data - in production, fetch from weather service
    this.weatherData = {
      location: '12.9034, 77.8256 (Bangalore, India)',
      description: "Here's a 7-day farming weather forecast for your coordinates (12.9034, 77.8256) in Bangalore, India, along with advice for irrigation and spraying.",
      forecast: {
        day: 'Saturday, February 28, 2026',
        condition: 'Partly sunny during the day and partly cloudy at night. The chance of rain is around 10%. Temperatures will range from approximately 18°C (66°F) to 31°C (87°F).',
        temp: '18°C - 31°C'
      }
    };
  }

  goToWeather() {
    this.router.navigate(['/weather']);
  }
}