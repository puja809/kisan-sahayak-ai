import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

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
export class HomeComponent { }