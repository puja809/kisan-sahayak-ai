import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SchemeService, Scheme } from '../../services/scheme.service';
import { GovernmentBodyService, GovernmentBody } from '../../services/government-body.service';
import { StateDistrictService, State, District } from '../../services/state-district.service';
import { GeolocationService } from '../../services/geolocation.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-schemes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schemes.component.html',
  styleUrls: ['./schemes.component.css']
})
export class SchemesComponent implements OnInit, OnDestroy {
  activeTab: 'schemes' | 'government-bodies' = 'schemes';
  
  // Location data
  selectedState: string = '';
  selectedDistrict: string = '';
  states: State[] = [];
  districts: District[] = [];
  
  // Schemes data
  schemes: Scheme[] = [];
  filteredSchemes: Scheme[] = [];
  
  // Government bodies data
  governmentBodies: GovernmentBody[] = [];
  filteredBodies: GovernmentBody[] = [];
  
  // Loading states
  loadingStates = false;
  loadingDistricts = false;
  loadingSchemes = false;
  loadingBodies = false;
  
  // Error messages
  errorMessage: string = '';
  
  private destroy$ = new Subject<void>();

  constructor(
    private schemeService: SchemeService,
    private governmentBodyService: GovernmentBodyService,
    private stateDistrictService: StateDistrictService,
    private geolocationService: GeolocationService
  ) {}

  ngOnInit(): void {
    this.loadStates();
    this.autoPopulateLocation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadStates(): void {
    this.loadingStates = true;
    this.stateDistrictService.getAllStates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (states) => {
          this.states = states.filter(s => s.isActive);
          this.loadingStates = false;
        },
        error: (err) => {
          console.error('Error loading states:', err);
          this.errorMessage = 'Failed to load states';
          this.loadingStates = false;
        }
      });
  }

  onStateChange(): void {
    this.selectedDistrict = '';
    this.districts = [];
    this.filteredSchemes = [];
    this.filteredBodies = [];
    
    if (!this.selectedState) return;

    this.loadingDistricts = true;
    this.stateDistrictService.getDistrictsByState(this.selectedState)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (districts) => {
          this.districts = districts.filter(d => d.isActive);
          this.loadingDistricts = false;
          this.loadDataForSelectedLocation();
        },
        error: (err) => {
          console.error('Error loading districts:', err);
          this.errorMessage = 'Failed to load districts';
          this.loadingDistricts = false;
        }
      });
  }

  onDistrictChange(): void {
    this.loadDataForSelectedLocation();
  }

  private loadDataForSelectedLocation(): void {
    if (!this.selectedState) return;

    if (this.activeTab === 'schemes') {
      this.loadSchemes();
    } else {
      this.loadGovernmentBodies();
    }
  }

  private loadSchemes(): void {
    if (!this.selectedState) return;

    this.loadingSchemes = true;
    const schemeCall = this.selectedDistrict
      ? this.schemeService.getSchemesByStateAndDistrict(this.selectedState, this.selectedDistrict)
      : this.schemeService.getSchemesByState(this.selectedState);

    schemeCall
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (schemes) => {
          this.filteredSchemes = schemes;
          this.loadingSchemes = false;
        },
        error: (err) => {
          console.error('Error loading schemes:', err);
          this.errorMessage = 'Failed to load schemes';
          this.loadingSchemes = false;
        }
      });
  }

  private loadGovernmentBodies(): void {
    if (!this.selectedState) return;

    this.loadingBodies = true;
    const bodiesCall = this.selectedDistrict
      ? this.governmentBodyService.getByStateAndDistrict(this.selectedState, this.selectedDistrict)
      : this.governmentBodyService.getByState(this.selectedState);

    bodiesCall
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (bodies) => {
          this.filteredBodies = bodies;
          this.loadingBodies = false;
        },
        error: (err) => {
          console.error('Error loading government bodies:', err);
          this.errorMessage = 'Failed to load government bodies';
          this.loadingBodies = false;
        }
      });
  }

  private autoPopulateLocation(): void {
    if (!this.geolocationService.isGeolocationSupported()) {
      console.log('Geolocation not supported');
      return;
    }

    this.geolocationService.getCurrentLocation()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (location) => {
          this.getAddressFromCoordinates(location.latitude, location.longitude);
        },
        error: (err) => {
          console.log('Geolocation error:', err);
        }
      });
  }

  private getAddressFromCoordinates(lat: number, lng: number): void {
    this.geolocationService.getAddressFromCoordinates(lat, lng)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (address) => {
          this.parseAddressAndSetLocation(address);
        },
        error: (err) => {
          console.log('Error getting address:', err);
        }
      });
  }

  private parseAddressAndSetLocation(address: string): void {
    // Parse address to extract state and district
    // Address format typically contains state and district information
    const parts = address.split(',').map(p => p.trim());
    
    // Try to match state from the address
    const matchedState = this.states.find(s => 
      parts.some(part => part.toLowerCase().includes(s.stateName.toLowerCase()))
    );

    if (matchedState) {
      this.selectedState = matchedState.stateName;
      this.onStateChange();
    }
  }

  switchTab(tab: 'schemes' | 'government-bodies'): void {
    this.activeTab = tab;
    this.errorMessage = '';
    
    if (this.selectedState) {
      this.loadDataForSelectedLocation();
    }
  }

  get isLoading(): boolean {
    return this.loadingSchemes || this.loadingBodies;
  }
}
