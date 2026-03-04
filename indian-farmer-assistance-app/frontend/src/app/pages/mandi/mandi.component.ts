import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, lastValueFrom } from 'rxjs';
import { MandiService, MandiPriceDto, FertilizerSupplierDto } from '../../services/mandi.service';
import { GeolocationService } from '../../services/geolocation.service';

@Component({
  selector: 'app-mandi',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mandi.component.html',
  styleUrls: ['./mandi.component.css'],})
export class MandiComponent implements OnInit {
  activeTab: 'prices' | 'fertilizers' = 'prices';

  // --- Mandi Prices State (Hierarchical Filters) ---
  filterOptions = {
    states: [] as string[],
    districts: [] as string[],
    markets: [] as string[],
    commodities: [] as string[],
    varieties: [] as string[],
    grades: [] as string[]
  };

  searchFilters = {
    state: '',
    district: '',
    market: '',
    commodity: '',
    variety: '',
    grade: ''
  };

  searchResults: MandiPriceDto[] = [];
  isLoadingPrices = false;
  hasSearchedPrices = false;
  errorMessagePrices = '';
  searchOffset = 0;
  searchLimit = 20;
  hasMorePrices = false;

  // --- Fertilizer State ---
  filterState: string = '';
  filterDistrict: string = '';
  fertilizers: FertilizerSupplierDto[] = [];
  isLoadingFertilizers = false;
  errorMessageFertilizers = '';
  hasSearchedFertilizers = false;
  supplierFilter: 'ALL' | 'WHOLESALER' | 'RETAILER' = 'ALL';

  constructor(private mandiService: MandiService, private http: HttpClient, private geolocationService: GeolocationService) { }

  ngOnInit() {
    this.detectUserLocationAndInit();
  }

  switchTab(tab: 'prices' | 'fertilizers') {
    this.activeTab = tab;
  }

  // ============== GEOLOCATION ==============

  detectUserLocationAndInit() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(async (position) => {
        try {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          const address = await firstValueFrom(this.geolocationService.getAddressFromCoordinates(lat, lng));

          if (address) {
            const parts = address.split(',').map(p => p.trim());
            // Expected format from our service: "District, State, Country"
            const detectedDistrict = parts.length > 0 && parts[0] ? parts[0] : 'Pune';
            const detectedState = parts.length > 1 && parts[1] ? parts[1] : 'Maharashtra';

            // Setup fertilizer location
            this.filterState = detectedState;
            this.filterDistrict = detectedDistrict;
            this.fetchFertilizers();

            // Setup mandi filter location
            this.searchFilters.state = detectedState;
            this.searchFilters.district = detectedDistrict;

            await this.initMandiFilters(detectedState, detectedDistrict);
          } else {
            this.initMandiFilters();
          }
        } catch (e) {
          console.warn('Reverse geocode failed', e);
          this.initMandiFilters();
        }
      }, () => {
        this.initMandiFilters();
      });
    } else {
      this.initMandiFilters();
    }
  }

  // ============== MANDI FILTER LOGIC ==============

  async initMandiFilters(detectedState?: string, detectedDistrict?: string) {
    try {
      this.filterOptions.states = await lastValueFrom(this.mandiService.getFilterStates());
      this.filterOptions.commodities = await lastValueFrom(this.mandiService.getFilterCommodities());

      if (detectedState && this.filterOptions.states.includes(detectedState)) {
        this.onStateChange();
      } else {
        this.searchFilters.state = '';
        this.searchFilters.district = '';
      }
    } catch (err) {
      console.error('Failed to init filters', err);
    }
  }

  onStateChange() {
    this.searchFilters.district = '';
    this.searchFilters.market = '';
    this.filterOptions.districts = [];
    this.filterOptions.markets = [];

    if (this.searchFilters.state) {
      this.mandiService.getFilterDistricts(this.searchFilters.state).subscribe({
        next: (data) => {
          this.filterOptions.districts = data;
          // If we had a detected district and it exists in the new list, select it
          if (this.searchFilters.district && data.includes(this.searchFilters.district)) {
            this.onDistrictChange();
          }
        },
        error: (err) => console.error('Failed to load districts', err)
      });
    }
  }

  onDistrictChange() {
    this.searchFilters.market = '';
    this.filterOptions.markets = [];

    if (this.searchFilters.state && this.searchFilters.district) {
      this.mandiService.getFilterMarkets(this.searchFilters.state, this.searchFilters.district).subscribe({
        next: (data) => this.filterOptions.markets = data,
        error: (err) => console.error('Failed to load markets', err)
      });
    }
  }

  onFertilizerStateChange() {
    this.filterDistrict = '';
    this.filterOptions.districts = [];

    if (this.filterState) {
      this.mandiService.getFilterDistricts(this.filterState).subscribe({
        next: (data) => {
          this.filterOptions.districts = data;
        },
        error: (err) => console.error('Failed to load districts', err)
      });
    }
  }

  fetchFilterCommodities() {
    this.searchFilters.variety = '';
    this.searchFilters.grade = '';
    this.filterOptions.varieties = [];
    this.filterOptions.grades = [];

    this.mandiService.getFilterCommodities(this.searchFilters.market).subscribe({
      next: (data) => this.filterOptions.commodities = data,
      error: (err) => console.error('Failed to load commodities', err)
    });
  }

  fetchFilterVarieties() {
    this.searchFilters.variety = '';
    this.searchFilters.grade = '';
    this.filterOptions.varieties = [];
    this.filterOptions.grades = [];

    if (this.searchFilters.commodity) {
      this.mandiService.getFilterVarieties(this.searchFilters.commodity).subscribe({
        next: (data) => this.filterOptions.varieties = data,
        error: (err) => console.error('Failed to load varieties', err)
      });
      this.fetchFilterGrades();
    }
  }

  fetchFilterGrades() {
    this.searchFilters.grade = '';
    this.filterOptions.grades = [];

    if (this.searchFilters.commodity) {
      this.mandiService.getFilterGrades(this.searchFilters.commodity, this.searchFilters.variety).subscribe({
        next: (data) => this.filterOptions.grades = data,
        error: (err) => console.error('Failed to load grades', err)
      });
    }
  }

  searchMarketData(isLoadMore = false) {
    if (!isLoadMore) {
      this.searchOffset = 0;
      this.searchResults = [];
    }

    this.isLoadingPrices = true;
    this.errorMessagePrices = '';
    this.hasSearchedPrices = true;

    this.mandiService.searchMarketData(this.searchFilters, this.searchOffset, this.searchLimit).subscribe({
      next: (data) => {
        if (isLoadMore) {
          this.searchResults = [...this.searchResults, ...data];
        } else {
          this.searchResults = data;
        }
        this.isLoadingPrices = false;
        this.hasMorePrices = data.length === this.searchLimit;
      },
      error: (err) => {
        this.errorMessagePrices = 'Failed to fetch market data based on selected filters.';
        this.isLoadingPrices = false;
        console.error(err);
      }
    });
  }

  loadMorePrices() {
    this.searchOffset += this.searchLimit;
    this.searchMarketData(true);
  }

  // ============== FERTILIZER LOGIC ==============

  fetchFertilizers() {
    if (!this.filterState || !this.filterDistrict) return;

    this.isLoadingFertilizers = true;
    this.errorMessageFertilizers = '';
    this.fertilizers = [];
    this.hasSearchedFertilizers = true;

    this.mandiService.getFertilizerSuppliersByLocation(this.filterState, this.filterDistrict).subscribe({
      next: (data) => {
        this.fertilizers = Array.isArray(data) ? data : [data];
        this.isLoadingFertilizers = false;
      },
      error: (err) => {
        console.error('Fertilizer location fetch error:', err);
        this.fallbackFertilizerSearch();
      }
    });
  }

  private fallbackFertilizerSearch() {
    this.mandiService.getFertilizerSuppliers(this.filterState, this.filterDistrict).subscribe({
      next: (data) => {
        this.fertilizers = Array.isArray(data) ? data : (data ? [data] : []);
        this.isLoadingFertilizers = false;
      },
      error: (err) => {
        this.errorMessageFertilizers = 'Failed to load fertilizer suppliers for this location.';
        this.isLoadingFertilizers = false;
      }
    });
  }

  setSupplierFilter(filter: 'ALL' | 'WHOLESALER' | 'RETAILER') {
    this.supplierFilter = filter;
  }

  getFilteredSuppliers(): FertilizerSupplierDto[] {
    if (this.supplierFilter === 'ALL') return this.fertilizers;
    if (this.supplierFilter === 'WHOLESALER') return this.fertilizers.filter(f => f.noOfWholesalers > 0);
    if (this.supplierFilter === 'RETAILER') return this.fertilizers.filter(f => f.noOfRetailers > 0);
    return this.fertilizers;
  }
}
