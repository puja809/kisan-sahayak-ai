import { Component, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeolocationService } from '../../services/geolocation.service';

@Component({
    selector: 'app-location-modal',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './location-modal.component.html',
    styleUrls: ['./location-modal.component.css'],})
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
