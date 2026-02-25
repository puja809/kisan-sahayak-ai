import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-weather',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="weather-container">
      <h2>Weather Forecast</h2>
      <p class="loading">Loading weather data...</p>
    </div>
  `,
  styles: [`
    .weather-container {
      padding: 1rem 0;
    }
    
    h2 {
      color: #2E7D32;
      margin-bottom: 1rem;
    }
    
    .loading {
      text-align: center;
      color: #757575;
      padding: 2rem;
    }
  `]
})
export class WeatherComponent {}