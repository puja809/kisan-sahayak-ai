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
  template: `
    <div class="app-container">
      <header class="app-header">
        <div class="header-content">
          <h1 class="app-title" routerLink="/">🌾 Kisan Sahayak</h1>
          <button class="hamburger" (click)="menuOpen = !menuOpen"
                  [class.active]="menuOpen">
            <span></span><span></span><span></span>
          </button>
          <nav class="app-nav" [class.open]="menuOpen">
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" class="nav-link" (click)="menuOpen=false">🏠 Home</a>
            
            <!-- Show only when NOT logged in -->
            <a routerLink="/weather" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">🌤️ Weather</a>
            <a routerLink="/crops" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">🌾 Crops</a>
            <a routerLink="/yield-calculator" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">📈 Yield</a>
            <a routerLink="/schemes" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">📋 Schemes</a>
            <a routerLink="/mandi" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">💰 Mandi</a>
            <a routerLink="/voice" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">🎤 Voice</a>
            <a routerLink="/disease" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">🦠 Disease</a>
            <a routerLink="/iot" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">📡 IoT</a>
            
            <!-- Show only when logged in -->
            <a *ngIf="isLoggedIn" routerLink="/dashboard" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">📊 Dashboard</a>
            <button class="nav-link location-btn" (click)="openLocationModal(); menuOpen=false">
              📍 {{ currentLocationName || 'Set Location' }}
            </button>
            <a *ngIf="isAdmin" routerLink="/admin" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">⚙️ Admin</a>
            
            <!-- Auth Nav -->
            <ng-container *ngIf="!isLoggedIn">
              <a routerLink="/login" routerLinkActive="active" class="nav-link" (click)="menuOpen=false">👤 Login</a>
            </ng-container>
            <ng-container *ngIf="isLoggedIn">
              <button class="nav-link" (click)="logout(); menuOpen=false" style="color: #ffcccc;">🚪 Logout ({{userName}})</button>
            </ng-container>
          </nav>
        </div>
      </header>
      <main class="app-main">
        <router-outlet></router-outlet>
      </main>
      <footer class="app-footer">
        <p>&copy; 2024 Kisan Sahayak AI — Indian Farmer Assistance. All rights reserved.</p>
      </footer>
    </div>
    
    <app-location-modal #locationModal></app-location-modal>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    .app-header {
      background: linear-gradient(135deg, #1B5E20 0%, #2E7D32 50%, #388E3C 100%);
      color: white;
      padding: 0.75rem 1rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header-content {
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .app-title {
      font-size: 1.4rem;
      font-weight: 600;
      margin: 0;
      cursor: pointer;
      white-space: nowrap;
    }

    .hamburger {
      display: none;
      flex-direction: column;
      gap: 4px;
      background: none;
      border: none;
      cursor: pointer;
      padding: 6px;
    }
    .hamburger span {
      display: block;
      width: 24px;
      height: 2px;
      background: white;
      transition: transform 0.3s;
    }
    .hamburger.active span:nth-child(1) { transform: rotate(45deg) translate(4px, 4px); }
    .hamburger.active span:nth-child(2) { opacity: 0; }
    .hamburger.active span:nth-child(3) { transform: rotate(-45deg) translate(4px, -4px); }

    .app-nav {
      display: flex;
      gap: 0.25rem;
      flex-wrap: wrap;
      align-items: center;
    }

    .nav-link {
      color: rgba(255,255,255,0.85);
      text-decoration: none;
      padding: 0.4rem 0.7rem;
      border-radius: 6px;
      transition: all 0.2s;
      font-size: 0.85rem;
      white-space: nowrap;
      background: transparent;
      border: none;
      cursor: pointer;
      font-family: inherit;
    }
    .nav-link:hover { background: rgba(255,255,255,0.15); color: white; }
    .nav-link.active { background: rgba(255,255,255,0.25); color: white; font-weight: 600; }
    
    .location-btn {
      background: rgba(255,255,255,0.1);
      border: 1px solid rgba(255,255,255,0.2);
    }

    .app-main {
      flex: 1;
      padding: 1.5rem 1rem;
      max-width: 1400px;
      margin: 0 auto;
      width: 100%;
    }

    .app-footer {
      background-color: #f5f5f5;
      padding: 1rem;
      text-align: center;
      color: #757575;
      font-size: 0.85rem;
    }

    @media (max-width: 900px) {
      .hamburger { display: flex; }
      .app-nav {
        display: none;
        flex-direction: column;
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background: #1B5E20;
        padding: 0.5rem;
        box-shadow: 0 4px 8px rgba(0,0,0,0.2);
      }
      .app-nav.open { display: flex; }
      .nav-link { width: 100%; padding: 0.7rem 1rem; text-align: left; }
    }
  `]
})
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
