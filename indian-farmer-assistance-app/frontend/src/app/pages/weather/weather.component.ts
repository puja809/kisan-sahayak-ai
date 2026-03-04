import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { WeatherService, CurrentWeatherDto, SevenDayForecastDto, AgrometAdvisoryDto } from '../../services/weather.service';
import { GeolocationService } from '../../services/geolocation.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-weather',
  standalone: true,
  imports: [CommonModule],
  providers: [DatePipe],
  templateUrl: './weather.component.html',
  styleUrls: ['./weather.component.css'],
})
export class WeatherComponent implements OnInit {
  isLoading = true;
  errorMessage = '';

  // State
  currentDistrict = 'Pune'; // Default
  currentWeather?: CurrentWeatherDto;
  forecast?: SevenDayForecastDto;
  advisories?: AgrometAdvisoryDto;

  currentLat?: number;
  currentLon?: number;

  constructor(
    private weatherService: WeatherService,
    private http: HttpClient,
    private geolocationService: GeolocationService
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
            // Reverse geocode using the centralized Geolocation Service
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            this.currentLat = lat;
            this.currentLon = lng;
            const address = await firstValueFrom(
              this.geolocationService.getAddressFromCoordinates(lat, lng)
            );

            // Extract district/city from the formatted address string (e.g. "Pune, Maharashtra, India")
            if (address) {
              const parts = address.split(',');
              if (parts.length > 0 && parts[0].trim()) {
                this.currentDistrict = parts[0].trim();
              }
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

    this.weatherService.getCurrentWeather(this.currentDistrict, undefined, this.currentLat, this.currentLon).subscribe({
      next: (data) => { this.currentWeather = data; checkDone(); },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to load weather data from backend.';
        checkDone();
      }
    });

    this.weatherService.getSevenDayForecast(this.currentDistrict, undefined, this.currentLat, this.currentLon).subscribe({
      next: (data) => { this.forecast = data; checkDone(); },
      error: () => checkDone()
    });

    this.weatherService.getAgrometAdvisories(this.currentDistrict, undefined, this.currentLat, this.currentLon).subscribe({
      next: (data) => { this.advisories = data; checkDone(); },
      error: () => checkDone()
    });
  }
}
