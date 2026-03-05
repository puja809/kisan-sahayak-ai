import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { GeolocationService } from '../../services/geolocation.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],})
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