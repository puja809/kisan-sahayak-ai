import { Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';

export interface UserLocation {
  latitude: number;
  longitude: number;
  accuracy?: number;
  altitude?: number | null;
  altitudeAccuracy?: number | null;
  heading?: number | null;
  speed?: number | null;
  timestamp: number;
}

export interface LocationError {
  code: number;
  message: string;
  timestamp: number;
}

export interface LocationPermissionStatus {
  granted: boolean;
  denied: boolean;
  prompt: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class GeolocationService {
  private currentLocation$ = new BehaviorSubject<UserLocation | null>(null);
  private locationError$ = new BehaviorSubject<LocationError | null>(null);
  private isTracking$ = new BehaviorSubject<boolean>(false);
  private savedAddress$ = new BehaviorSubject<string | null>(null);
  private watchId: number | null = null;

  constructor() {
    this.checkGeolocationSupport();
    this.loadSavedAddress();
  }

  private loadSavedAddress(): void {
    const saved = localStorage.getItem('manual_location');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed.address) {
          this.savedAddress$.next(parsed.address);
        }
      } catch (e) {
        console.error('Error parsing saved location', e);
      }
    }
  }

  getSavedAddress(): Observable<string | null> {
    return this.savedAddress$.asObservable();
  }

  setSavedAddress(address: string, coords?: { lat: number, lng: number }): void {
    const locationData = coords
      ? { address, latitude: coords.lat, longitude: coords.lng, source: 'manual-coords' }
      : { address, source: 'manual-name' };

    localStorage.setItem('manual_location', JSON.stringify(locationData));
    this.savedAddress$.next(address);
  }

  /**
   * Check if browser supports geolocation
   */
  isGeolocationSupported(): boolean {
    return 'geolocation' in navigator;
  }

  /**
   * Check geolocation support on initialization
   */
  private checkGeolocationSupport(): void {
    if (!this.isGeolocationSupported()) {
      console.warn('Geolocation is not supported by this browser');
    }
  }

  /**
   * Get current user location (one-time)
   */
  getCurrentLocation(): Observable<UserLocation> {
    return new Observable(observer => {
      if (!this.isGeolocationSupported()) {
        const error: LocationError = {
          code: 0,
          message: 'Geolocation is not supported by this browser',
          timestamp: Date.now()
        };
        this.locationError$.next(error);
        observer.error(error);
        return;
      }

      const options: PositionOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      };

      navigator.geolocation.getCurrentPosition(
        (position: GeolocationPosition) => {
          const location: UserLocation = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            altitude: position.coords.altitude,
            altitudeAccuracy: position.coords.altitudeAccuracy,
            heading: position.coords.heading,
            speed: position.coords.speed,
            timestamp: position.timestamp
          };
          this.currentLocation$.next(location);
          this.locationError$.next(null);
          observer.next(location);
          observer.complete();
        },
        (error: GeolocationPositionError) => {
          const locationError: LocationError = {
            code: error.code,
            message: this.getErrorMessage(error.code),
            timestamp: Date.now()
          };
          this.locationError$.next(locationError);
          observer.error(locationError);
        },
        options
      );
    });
  }

  /**
   * Watch user location continuously
   */
  watchLocation(): Observable<UserLocation> {
    return new Observable(observer => {
      if (!this.isGeolocationSupported()) {
        const error: LocationError = {
          code: 0,
          message: 'Geolocation is not supported by this browser',
          timestamp: Date.now()
        };
        this.locationError$.next(error);
        observer.error(error);
        return;
      }

      // Stop previous watch if any
      if (this.watchId !== null) {
        navigator.geolocation.clearWatch(this.watchId);
      }

      const options: PositionOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 1000
      };

      this.watchId = navigator.geolocation.watchPosition(
        (position: GeolocationPosition) => {
          const location: UserLocation = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            altitude: position.coords.altitude,
            altitudeAccuracy: position.coords.altitudeAccuracy,
            heading: position.coords.heading,
            speed: position.coords.speed,
            timestamp: position.timestamp
          };
          this.currentLocation$.next(location);
          this.locationError$.next(null);
          this.isTracking$.next(true);
          observer.next(location);
        },
        (error: GeolocationPositionError) => {
          const locationError: LocationError = {
            code: error.code,
            message: this.getErrorMessage(error.code),
            timestamp: Date.now()
          };
          this.locationError$.next(locationError);
          this.isTracking$.next(false);
          observer.error(locationError);
        },
        options
      );

      // Return unsubscribe function
      return () => {
        if (this.watchId !== null) {
          navigator.geolocation.clearWatch(this.watchId);
          this.watchId = null;
          this.isTracking$.next(false);
        }
      };
    });
  }

  /**
   * Stop watching location
   */
  stopWatchingLocation(): void {
    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
      this.isTracking$.next(false);
    }
  }

  /**
   * Get current location as observable
   */
  getCurrentLocationObservable(): Observable<UserLocation | null> {
    return this.currentLocation$.asObservable();
  }

  /**
   * Get location error as observable
   */
  getLocationError(): Observable<LocationError | null> {
    return this.locationError$.asObservable();
  }

  /**
   * Get tracking status as observable
   */
  isTracking(): Observable<boolean> {
    return this.isTracking$.asObservable();
  }

  /**
   * Get current location value synchronously
   */
  getCurrentLocationValue(): UserLocation | null {
    return this.currentLocation$.value;
  }

  /**
   * Get current error value synchronously
   */
  getCurrentErrorValue(): LocationError | null {
    return this.locationError$.value;
  }

  /**
   * Get tracking status value synchronously
   */
  isTrackingValue(): boolean {
    return this.isTracking$.value;
  }

  /**
   * Calculate distance between two coordinates (Haversine formula)
   */
  calculateDistance(
    lat1: number,
    lon1: number,
    lat2: number,
    lon2: number
  ): number {
    const R = 6371; // Earth's radius in kilometers
    const dLat = this.toRad(lat2 - lat1);
    const dLon = this.toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(lat1)) *
      Math.cos(this.toRad(lat2)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c; // Distance in kilometers
  }

  /**
   * Calculate bearing between two coordinates
   */
  calculateBearing(
    lat1: number,
    lon1: number,
    lat2: number,
    lon2: number
  ): number {
    const dLon = this.toRad(lon2 - lon1);
    const y = Math.sin(dLon) * Math.cos(this.toRad(lat2));
    const x =
      Math.cos(this.toRad(lat1)) * Math.sin(this.toRad(lat2)) -
      Math.sin(this.toRad(lat1)) *
      Math.cos(this.toRad(lat2)) *
      Math.cos(dLon);
    const bearing = Math.atan2(y, x);
    return (this.toDeg(bearing) + 360) % 360; // Bearing in degrees
  }

  /**
   * Get address from coordinates (reverse geocoding)
   * Note: This requires a backend service or third-party API
   */
  getAddressFromCoordinates(
    latitude: number,
    longitude: number
  ): Observable<string> {
    return new Observable(observer => {
      // This would typically call a backend service
      // For now, returning a formatted coordinate string
      const address = `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
      observer.next(address);
      observer.complete();
    });
  }

  /**
   * Format location for display
   */
  formatLocation(location: UserLocation): string {
    return `Lat: ${location.latitude.toFixed(6)}, Lon: ${location.longitude.toFixed(6)}`;
  }

  /**
   * Format location with accuracy
   */
  formatLocationWithAccuracy(location: UserLocation): string {
    const accuracy = location.accuracy
      ? ` (±${location.accuracy.toFixed(0)}m)`
      : '';
    return `${this.formatLocation(location)}${accuracy}`;
  }

  /**
   * Check if location is within bounds
   */
  isLocationWithinBounds(
    location: UserLocation,
    minLat: number,
    maxLat: number,
    minLon: number,
    maxLon: number
  ): boolean {
    return (
      location.latitude >= minLat &&
      location.latitude <= maxLat &&
      location.longitude >= minLon &&
      location.longitude <= maxLon
    );
  }

  /**
   * Get error message from error code
   */
  private getErrorMessage(code: number): string {
    switch (code) {
      case 1:
        return 'Permission denied. Please enable location access in your browser settings.';
      case 2:
        return 'Position unavailable. Unable to retrieve your location.';
      case 3:
        return 'Request timeout. Location request took too long.';
      default:
        return 'An unknown error occurred while retrieving your location.';
    }
  }

  /**
   * Convert degrees to radians
   */
  private toRad(degrees: number): number {
    return (degrees * Math.PI) / 180;
  }

  /**
   * Convert radians to degrees
   */
  private toDeg(radians: number): number {
    return (radians * 180) / Math.PI;
  }

  /**
   * Clear all location data
   */
  clearLocationData(): void {
    this.stopWatchingLocation();
    this.currentLocation$.next(null);
    this.locationError$.next(null);
  }
}
