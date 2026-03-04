import { Component, OnInit, ViewChild } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LocationModalComponent } from './components/location-modal/location-modal.component';
import { GeolocationService } from './services/geolocation.service';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LocationModalComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],})
export class AppComponent implements OnInit {
  title = 'Kisan Sahayak AI';
  menuOpen = false;
  currentLocationName: string | null = null;
  isLoggedIn = false;
  isAdmin = false;
  userName = '';

  @ViewChild('locationModal') locationModal!: LocationModalComponent;

  constructor(private geolocationService: GeolocationService, private authService: AuthService) { }

  ngOnInit() {
    this.geolocationService.getSavedAddress().subscribe(address => {
      this.currentLocationName = address;
    });
    this.authService.currentUser$.subscribe(user => {
      this.isLoggedIn = !!user;
      this.isAdmin = user?.role === 'ADMIN';
      this.userName = user?.name || '';
    });
  }

  openLocationModal() {
    if (this.locationModal) {
      this.locationModal.open();
    }
  }

  logout() {
    this.authService.logout();
    this.menuOpen = false;
  }
}
