import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, lastValueFrom } from 'rxjs';
import { MandiService, MandiPriceDto, FertilizerSupplierDto } from '../../services/mandi.service';

@Component({
  selector: 'app-mandi',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="mandi-container">
      <div class="header-section">
        <h2>Market & Supplies</h2>
        <p>Stay updated with the latest agricultural commodity prices and local fertilizer suppliers.</p>
      </div>

      <!-- Navigation Tabs -->
      <div class="tabs">
        <button class="tab-btn" [class.active]="activeTab === 'prices'" (click)="switchTab('prices')">Mandi Prices</button>
        <button class="tab-btn" [class.active]="activeTab === 'fertilizers'" (click)="switchTab('fertilizers')">Fertilizer Suppliers</button>
      </div>

      <!-- TAB 1: Mandi Prices -->
      <div *ngIf="activeTab === 'prices'" class="tab-content">
        <div class="controls-section filters-grid">
          <div class="form-group">
            <label>State</label>
            <select [(ngModel)]="searchFilters.state" (change)="onStateChange()" class="input-field">
              <option value="">-- All States --</option>
              <option *ngFor="let state of filterOptions.states" [value]="state">{{ state }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>District</label>
            <select [(ngModel)]="searchFilters.district" (change)="onDistrictChange()" class="input-field" [disabled]="!searchFilters.state">
              <option value="">-- All Districts --</option>
              <option *ngFor="let district of filterOptions.districts" [value]="district">{{ district }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Market</label>
            <select [(ngModel)]="searchFilters.market" (change)="fetchFilterCommodities()" class="input-field" [disabled]="!searchFilters.district">
              <option value="">-- All Markets --</option>
              <option *ngFor="let market of filterOptions.markets" [value]="market">{{ market }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Commodity</label>
            <select [(ngModel)]="searchFilters.commodity" (change)="fetchFilterVarieties()" class="input-field">
              <option value="">-- All Commodities --</option>
              <option *ngFor="let comm of filterOptions.commodities" [value]="comm">{{ comm }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Variety</label>
            <select [(ngModel)]="searchFilters.variety" (change)="fetchFilterGrades()" class="input-field" [disabled]="!searchFilters.commodity">
              <option value="">-- All Varieties --</option>
              <option *ngFor="let varietyItem of filterOptions.varieties" [value]="varietyItem">{{ varietyItem }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Grade</label>
            <select [(ngModel)]="searchFilters.grade" class="input-field" [disabled]="!searchFilters.commodity">
              <option value="">-- All Grades --</option>
              <option *ngFor="let grade of filterOptions.grades" [value]="grade">{{ grade }}</option>
            </select>
          </div>
          
          <div class="form-group" style="display: flex; align-items: flex-end;">
            <button class="btn-primary w-100" (click)="searchMarketData()" [disabled]="isLoadingPrices">
              {{ isLoadingPrices ? 'Searching...' : 'Search Market Data' }}
            </button>
          </div>
        </div>

        <div class="loader" *ngIf="isLoadingPrices">
          <div class="spinner"></div>
          <p>Fetching market data...</p>
        </div>

        <div class="error-message" *ngIf="errorMessagePrices">{{ errorMessagePrices }}</div>

        <div class="prices-grid" *ngIf="searchResults.length > 0">
          <div class="price-card" *ngFor="let result of searchResults">
            <div class="card-body">
              <h4 class="commodity-heading">{{ result.commodityName }} <span class="variety-badge" *ngIf="result.variety">{{ result.variety }}</span></h4>
              <p class="location-info">📍 {{ result.mandiName }}, {{ result.district }}, {{ result.state }}</p>
              <div class="price-details">
                <p><strong>Modal Price:</strong> ₹{{ result.modalPrice }} / {{ result.unit || 'Quintal' }}</p>
                <p class="price-range" *ngIf="result.minPrice && result.maxPrice"><strong>Range:</strong> ₹{{ result.minPrice }} - ₹{{ result.maxPrice }}</p>
              </div>
              <p class="date"><small>Reported: {{ result.priceDate }}</small></p>
            </div>
          </div>
        </div>
        
        <div class="text-center mt-3 mb-4" *ngIf="hasMorePrices && searchResults.length > 0">
          <button class="btn-secondary" (click)="loadMorePrices()" [disabled]="isLoadingPrices">
            {{ isLoadingPrices ? 'Loading more...' : 'Load More' }}
          </button>
        </div>
        
        <div class="empty-state" *ngIf="!isLoadingPrices && searchResults.length === 0 && hasSearchedPrices && !errorMessagePrices">
          <p>No market data found for the selected filters. Try adjusting your search criteria.</p>
        </div>
      </div>

      <!-- TAB 2: Fertilizer Suppliers -->
      <div *ngIf="activeTab === 'fertilizers'" class="tab-content">
        <div class="controls-section">
          <div class="form-group flex-row">
             <div class="flex-1">
               <label for="stateSelect">State</label>
               <select id="stateSelect" [(ngModel)]="filterState" (change)="onFertilizerStateChange()" class="input-field">
                 <option value="">-- All States --</option>
                 <option *ngFor="let state of filterOptions.states" [value]="state">{{ state }}</option>
               </select>
             </div>
             <div class="flex-1">
               <label for="districtSelect">District</label>
               <select id="districtSelect" [(ngModel)]="filterDistrict" class="input-field" [disabled]="!filterState">
                 <option value="">-- All Districts --</option>
                 <option *ngFor="let district of filterOptions.districts" [value]="district">{{ district }}</option>
               </select>
             </div>
          </div>
          <button class="btn-primary" (click)="fetchFertilizers()" [disabled]="!filterState || !filterDistrict || isLoadingFertilizers">
            {{ isLoadingFertilizers ? 'Searching...' : 'Find Suppliers' }}
          </button>
        </div>

        <div class="filter-pills" *ngIf="fertilizers.length > 0">
           <button class="pill" [class.active]="supplierFilter === 'ALL'" (click)="setSupplierFilter('ALL')">All Types</button>
           <button class="pill" [class.active]="supplierFilter === 'WHOLESALER'" (click)="setSupplierFilter('WHOLESALER')">Wholesalers</button>
           <button class="pill" [class.active]="supplierFilter === 'RETAILER'" (click)="setSupplierFilter('RETAILER')">Retailers</button>
        </div>

        <div class="loader" *ngIf="isLoadingFertilizers">
          <div class="spinner"></div>
          <p>Finding fertilizer suppliers in {{ filterDistrict }}, {{ filterState }}...</p>
        </div>

        <div class="error-message" *ngIf="errorMessageFertilizers">{{ errorMessageFertilizers }}</div>

        <div class="suppliers-list" *ngIf="!isLoadingFertilizers && getFilteredSuppliers().length > 0">
          <div class="supplier-card" *ngFor="let supplier of getFilteredSuppliers()">
            <div class="supplier-info">
              <h4>{{ supplier.supplierName || 'Unnamed Supplier' }}</h4>
              <p class="type-badge">{{ supplier.fertilizerType || 'General Fertilizer' }}</p>
              <p class="location">📍 {{ supplier.district }}, {{ supplier.state }}</p>
            </div>
            <div class="supplier-stats">
               <div class="stat-box" title="Wholesalers">📦 <strong>{{ supplier.noOfWholesalers || 0 }}</strong> <small>Wholesale</small></div>
               <div class="stat-box" title="Retailers">🏬 <strong>{{ supplier.noOfRetailers || 0 }}</strong> <small>Retail</small></div>
            </div>
          </div>
        </div>
        
        <div class="empty-state" *ngIf="!isLoadingFertilizers && getFilteredSuppliers().length === 0 && !errorMessageFertilizers && hasSearchedFertilizers">
          <p>No fertilizer suppliers found in {{ filterDistrict }}, {{ filterState }} for the selected filter.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mandi-container { padding: 1.5rem; max-width: 1200px; margin: 0 auto; }
    .header-section { text-align: center; margin-bottom: 2rem; }
    h2 { color: #2E7D32; font-size: 2rem; margin-bottom: 0.5rem; }
    
    .tabs { display: flex; justify-content: center; gap: 1rem; margin-bottom: 2rem; border-bottom: 2px solid #E8F5E9; padding-bottom: 1rem; }
    .tab-btn { background: none; border: none; font-size: 1.1rem; color: #757575; padding: 0.5rem 1.5rem; cursor: pointer; font-weight: 500; border-radius: 8px; transition: all 0.2s; }
    .tab-btn:hover { background: #f5f5f5; color: #2E7D32; }
    .tab-btn.active { background: #E8F5E9; color: #2E7D32; }

    .controls-section { background: white; padding: 1.5rem; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); margin-bottom: 2rem; }
    .filters-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; }
    
    .form-group { display: flex; flex-direction: column; }
    .flex-row { display: flex; gap: 1rem; width: 100%; margin-bottom: 1rem; }
    .flex-1 { flex: 1; min-width: 120px; }
    label { margin-bottom: 0.5rem; font-weight: 500; color: #424242; font-size: 0.9rem; }
    select, .input-field { width: 100%; padding: 0.75rem; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 0.95rem; outline: none; box-sizing: border-box; background: white; }
    select:focus, .input-field:focus { border-color: #2E7D32; }
    select:disabled { background: #f5f5f5; cursor: not-allowed; }
    
    .btn-primary { padding: 0.75rem 1.5rem; background: #2E7D32; color: white; border: none; border-radius: 8px; font-size: 1rem; font-weight: 500; cursor: pointer; transition: background 0.3s; white-space: nowrap; }
    .btn-primary.w-100 { width: 100%; }
    .btn-primary:hover:not(:disabled) { background: #1B5E20; }
    .btn-primary:disabled { background: #A5D6A7; cursor: not-allowed; }
    
    .filter-pills { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
    .pill { padding: 0.5rem 1rem; border-radius: 99px; border: 1px solid #e0e0e0; background: white; color: #616161; cursor: pointer; font-size: 0.85rem; font-weight: 500; }
    .pill.active { background: #2196F3; color: white; border-color: #2196F3; }
    
    .prices-grid, .suppliers-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.5rem; }
    .suppliers-list { display: flex; flex-direction: column; gap: 1rem; }
    
    .price-card, .supplier-card { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); display: flex; flex-direction: column; transition: transform 0.2s; }
    .price-card:hover { transform: translateY(-4px); }
    .price-card .card-body { padding: 1.5rem; border-left: 4px solid #2E7D32; border-radius: 12px; }
    .commodity-heading { margin: 0 0 0.5rem 0; color: #2E7D32; font-size: 1.25rem; display: flex; align-items: center; justify-content: space-between; }
    .variety-badge { background: #E8F5E9; color: #1B5E20; font-size: 0.8rem; padding: 0.2rem 0.6rem; border-radius: 99px; font-weight: 500; }
    .location-info { margin: 0 0 1rem 0; color: #616161; font-size: 0.95rem; }
    .price-details { background: #f9f9f9; padding: 0.75rem 1rem; border-radius: 8px; margin-bottom: 0.75rem; }
    .price-details p { margin: 0 0 0.25rem 0; font-size: 0.95rem; color: #424242; }
    .price-details p:last-child { margin-bottom: 0; }
    .date { margin: 0; color: #9e9e9e; text-align: right; }
    .text-center { text-align: center; }
    .mt-3 { margin-top: 1rem; }
    .mb-4 { margin-bottom: 1.5rem; }
    .btn-secondary { padding: 0.5rem 1.5rem; background: #fff; color: #2E7D32; border: 1px solid #2E7D32; border-radius: 8px; font-size: 0.95rem; font-weight: 500; cursor: pointer; transition: all 0.3s; }
    .btn-secondary:hover:not(:disabled) { background: #E8F5E9; }
    .btn-secondary:disabled { border-color: #A5D6A7; color: #A5D6A7; cursor: not-allowed; }
    
    .supplier-card { padding: 1.5rem; flex-direction: row; justify-content: space-between; align-items: center; border-left: 4px solid #FF9800; border-radius: 12px; }
    .supplier-card:hover { transform: translateY(-2px); }
    .supplier-info h4 { margin: 0 0 0.5rem 0; color: #333; font-size: 1.2rem; }
    .type-badge { display: inline-block; background: #FFF3E0; color: #E65100; padding: 0.2rem 0.6rem; border-radius: 4px; font-size: 0.8rem; font-weight: 600; margin: 0 0 0.5rem 0; }
    .location { margin: 0; color: #666; font-size: 0.9rem; }
    .supplier-stats { display: flex; gap: 1rem; }
    .stat-box { background: #f5f5f5; padding: 0.5rem 1rem; border-radius: 8px; text-align: center; color: #424242; }
    .stat-box strong { font-size: 1.2rem; display: block; }
    .stat-box small { font-size: 0.75rem; text-transform: uppercase; color: #757575; }

    .loader { text-align: center; padding: 3rem; }
    .spinner { border: 4px solid #f3f3f3; border-top: 4px solid #2E7D32; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto 1rem; }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    .error-message { background: #FFEBEE; color: #C62828; padding: 1rem; border-radius: 8px; text-align: center; margin-bottom: 1rem; }
    .empty-state { text-align: center; padding: 3rem; color: #757575; background: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }

    @media (max-width: 768px) {
      .filters-grid { grid-template-columns: 1fr; }
      .supplier-card { flex-direction: column; align-items: flex-start; gap: 1rem; }
    }
  `]
})
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

  constructor(private mandiService: MandiService, private http: HttpClient) { }

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
          const url = 'https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=' + lat + '&longitude=' + lng + '&localityLanguage=en';

          const response: any = await firstValueFrom(this.http.get(url));

          if (response) {
            const detectedState = response.principalSubdivision || 'Maharashtra';
            const detectedDistrict = response.city || response.locality || 'Pune';

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
