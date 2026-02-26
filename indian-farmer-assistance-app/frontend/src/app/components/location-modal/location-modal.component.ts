import { Component, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeolocationService } from '../../services/geolocation.service';

@Component({
    selector: 'app-location-modal',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="closeOnBackdrop($event)">
      <div class="modal-content">
        <div class="modal-header">
          <div class="modal-icon">🚜</div>
          <h2>Welcome, Farmer!</h2>
          <p>To give you the best crop advice, weather, and mandi prices, we need your location.</p>
        </div>

        <div class="modal-body">
          <button class="btn btn-use-location" (click)="useCurrentLocation()" [disabled]="isLoading">
            <span class="icon">📍</span>
            {{ isLoading ? 'Detecting...' : 'Use My Current Location' }}
          </button>

          <div class="divider">
            <span>OR</span>
          </div>

          <div class="manual-input-group">
            <div class="input-wrapper">
              <label>Enter Village / District</label>
              <input 
                type="text" 
                [(ngModel)]="manualLocation" 
                placeholder="e.g. Bathinda"
                (keyup.enter)="saveManualLocation()"
              >
            </div>
            <button class="btn btn-set" (click)="saveManualLocation()" [disabled]="!manualLocation.trim()">
              Set
            </button>
          </div>
        </div>
        
        <button *ngIf="canClose" class="close-btn" (click)="close()">×</button>
      </div>
    </div>
  `,
    styles: [`
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      backdrop-filter: blur(4px);
    }

    .modal-content {
      background: white;
      border-radius: 12px;
      width: 90%;
      max-width: 450px;
      padding: 30px;
      position: relative;
      box-shadow: 0 10px 30px rgba(0,0,0,0.2);
    }

    .close-btn {
      position: absolute;
      top: 15px;
      right: 15px;
      background: none;
      border: none;
      font-size: 1.5rem;
      color: #999;
      cursor: pointer;
      line-height: 1;
      padding: 5px;
      transition: color 0.2s;
    }
    .close-btn:hover { color: #333; }

    .modal-header {
      text-align: center;
      margin-bottom: 25px;
    }
    
    .modal-icon {
      font-size: 3rem;
      margin-bottom: 10px;
    }

    h2 {
      color: #2E7D32;
      margin: 0 0 10px 0;
      font-size: 1.8rem;
    }

    p {
      color: #666;
      margin: 0;
      font-size: 0.95rem;
      line-height: 1.4;
    }

    .modal-body {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .btn {
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      font-size: 1rem;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-use-location {
      background: #e8f5e9;
      color: #2E7D32;
      padding: 14px;
      width: 100%;
      border: 1px solid #c8e6c9;
    }
    .btn-use-location:hover:not(:disabled) {
      background: #c8e6c9;
    }

    .divider {
      display: flex;
      align-items: center;
      text-align: center;
      color: #999;
      font-size: 0.85rem;
    }
    .divider::before, .divider::after {
      content: '';
      flex: 1;
      border-bottom: 1px solid #eee;
    }
    .divider span {
      padding: 0 15px;
    }

    .manual-input-group {
      display: flex;
      align-items: flex-end;
      gap: 10px;
    }

    .input-wrapper {
      flex: 1;
      display: flex;
      flex-direction: column;
      text-align: left;
    }

    .input-wrapper label {
      font-size: 0.8rem;
      color: #555;
      margin-bottom: 6px;
      font-weight: 500;
    }

    .input-wrapper input {
      padding: 12px 15px;
      border: 1px solid #ccc;
      border-radius: 8px;
      font-size: 1rem;
      background: #fafafa;
      color: #333;
    }
    .input-wrapper input:focus {
      outline: none;
      border-color: #2E7D32;
      background: white;
      box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.1);
    }

    .btn-set {
      background: #81c784;
      color: white;
      padding: 12px 24px;
      height: 44px; /* Matches input height roughly */
      border-radius: 8px;
    }
    .btn-set:hover:not(:disabled) {
      background: #66bb6a;
    }
  `]
})
export class LocationModalComponent implements OnInit {
    isOpen = false;
    canClose = false;
    isLoading = false;
    manualLocation = '';

    @Output() locationSet = new EventEmitter<void>();

    constructor(private geolocationService: GeolocationService) { }

    ngOnInit() {
        this.checkLocation();
    }

    open(force = false) {
        this.manualLocation = '';
        this.canClose = !force;
        this.isOpen = true;
    }

    close() {
        if (this.canClose) {
            this.isOpen = false;
        }
    }

    closeOnBackdrop(event: MouseEvent) {
        if ((event.target as HTMLElement).className === 'modal-overlay' && this.canClose) {
            this.close();
        }
    }

    checkLocation() {
        // If no location is saved, open modal and force them to enter it
        const saved = localStorage.getItem('manual_location');
        if (!saved) {
            this.open(true); // force = true (cannot close without setting)
        }
    }

    useCurrentLocation() {
        this.isLoading = true;
        this.geolocationService.getCurrentLocation().subscribe({
            next: (loc) => {
                this.isLoading = false;
                // Call a geocoding API or just use coordinates as name
                const locName = `${loc.latitude.toFixed(4)}°N, ${loc.longitude.toFixed(4)}°E`;
                this.geolocationService.setSavedAddress(locName, { lat: loc.latitude, lng: loc.longitude });
                this.isOpen = false;
                this.locationSet.emit();
            },
            error: () => {
                this.isLoading = false;
                alert('Could not get your location. Please enter it manually.');
            }
        });
    }

    saveManualLocation() {
        if (this.manualLocation.trim()) {
            this.geolocationService.setSavedAddress(this.manualLocation.trim());
            this.isOpen = false;
            this.locationSet.emit();
        }
    }
}
