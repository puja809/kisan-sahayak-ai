import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
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
        <div class="controls-section">
          <div class="form-group">
            <label for="commoditySelect">Select Commodity:</label>
            <select id="commoditySelect" [(ngModel)]="selectedCommodity" (change)="fetchPrices()">
              <option value="">-- Choose Commodity --</option>
              <option *ngFor="let comm of commodities" [value]="comm">{{ comm }}</option>
            </select>
          </div>
          <button class="btn-primary" (click)="fetchPrices()" [disabled]="!selectedCommodity || isLoadingPrices">
            {{ isLoadingPrices ? 'Loading...' : 'Check Prices' }}
          </button>
        </div>

        <div class="loader" *ngIf="isLoadingPrices">
          <div class="spinner"></div>
          <p>Fetching market data...</p>
        </div>

        <div class="error-message" *ngIf="errorMessagePrices">{{ errorMessagePrices }}</div>

        <div class="prices-grid" *ngIf="!isLoadingPrices && prices.length > 0">
          <div class="price-card" *ngFor="let price of prices">
            <div class="card-header">
              <h3>{{ price.mandiName }}</h3>
              <span class="badge">{{ price.commodityName }}</span>
            </div>
            <div class="card-body">
              <p><strong>Variety:</strong> {{ price.variety || 'N/A' }}</p>
              <p><strong>Location:</strong> {{ price.district }}, {{ price.state }}</p>
              <p><strong>Arrival:</strong> {{ price.arrivalQuantityQuintals || 0 }} {{ price.unit || 'Quintals' }}</p>
              <div class="price-details">
                <div class="price-item">
                  <span class="label">Min Price</span><span class="value">₹{{ price.minPrice || 0 }}</span>
                </div>
                <div class="price-item highlight">
                  <span class="label">Modal Price</span><span class="value">₹{{ price.modalPrice || 0 }}</span>
                </div>
                <div class="price-item">
                  <span class="label">Max Price</span><span class="value">₹{{ price.maxPrice || 0 }}</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <small>Updated: {{ price.priceDate | date:'mediumDate' }}</small>
              <small *ngIf="price.distanceKm" class="distance">📍 {{ price.distanceKm | number:'1.0-1' }} km away</small>
            </div>
          </div>
        </div>
        
        <div class="empty-state" *ngIf="!isLoadingPrices && prices.length === 0 && selectedCommodity && !errorMessagePrices">
          <p>No prices found for {{ selectedCommodity }}. Try another commodity or check back later.</p>
        </div>
      </div>

      <!-- TAB 2: Fertilizer Suppliers -->
      <div *ngIf="activeTab === 'fertilizers'" class="tab-content">
        <div class="controls-section">
          <div class="form-group flex-row">
             <div class="flex-1">
               <label for="stateSelect">State</label>
               <input id="stateSelect" type="text" [(ngModel)]="filterState" placeholder="e.g. Maharashtra" class="input-field" (keyup.enter)="fetchFertilizers()">
             </div>
             <div class="flex-1">
               <label for="districtSelect">District</label>
               <input id="districtSelect" type="text" [(ngModel)]="filterDistrict" placeholder="e.g. Pune" class="input-field" (keyup.enter)="fetchFertilizers()">
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

    .controls-section { display: flex; flex-wrap: wrap; gap: 1rem; align-items: flex-end; background: white; padding: 1.5rem; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); margin-bottom: 2rem; }
    .form-group { flex: 1; min-width: 250px; }
    .flex-row { display: flex; gap: 1rem; }
    .flex-1 { flex: 1; min-width: 120px; }
    label { display: block; margin-bottom: 0.5rem; font-weight: 500; color: #424242; }
    select, .input-field { width: 100%; padding: 0.75rem; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 1rem; outline: none; box-sizing: border-box; }
    select:focus, .input-field:focus { border-color: #2E7D32; }
    
    .btn-primary { padding: 0.75rem 1.5rem; background: #2E7D32; color: white; border: none; border-radius: 8px; font-size: 1rem; font-weight: 500; cursor: pointer; transition: background 0.3s; white-space: nowrap; }
    .btn-primary:hover:not(:disabled) { background: #1B5E20; }
    .btn-primary:disabled { background: #A5D6A7; cursor: not-allowed; }
    
    .filter-pills { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
    .pill { padding: 0.5rem 1rem; border-radius: 99px; border: 1px solid #e0e0e0; background: white; color: #616161; cursor: pointer; font-size: 0.85rem; font-weight: 500; }
    .pill.active { background: #2196F3; color: white; border-color: #2196F3; }
    
    .prices-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.5rem; }
    .price-card { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); display: flex; flex-direction: column; transition: transform 0.2s; }
    .price-card:hover { transform: translateY(-4px); }
    .card-header { background: #E8F5E9; padding: 1rem; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #C8E6C9; }
    .card-header h3 { margin: 0; color: #2E7D32; font-size: 1.25rem; }
    .badge { background: #2E7D32; color: white; padding: 0.25rem 0.75rem; border-radius: 99px; font-size: 0.875rem; font-weight: 500; }
    .card-body { padding: 1rem; flex: 1; }
    .card-body p { margin: 0 0 0.5rem 0; color: #616161; font-size: 0.95rem; }
    .price-details { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0.5rem; margin-top: 1rem; padding-top: 1rem; border-top: 1px dashed #e0e0e0; }
    .price-item { text-align: center; }
    .price-item .label { display: block; font-size: 0.75rem; color: #757575; text-transform: uppercase; margin-bottom: 0.25rem; }
    .price-item .value { display: block; font-weight: 600; color: #424242; }
    .price-item.highlight .value { color: #2E7D32; font-size: 1.1rem; }
    .card-footer { padding: 0.75rem 1rem; background: #fafafa; border-top: 1px solid #eee; display: flex; justify-content: space-between; color: #9e9e9e; }
    .distance { color: #1976D2; font-weight: 500; }

    .suppliers-list { display: flex; flex-direction: column; gap: 1rem; }
    .supplier-card { background: white; border-radius: 12px; padding: 1.5rem; box-shadow: 0 4px 6px rgba(0,0,0,0.05); display: flex; justify-content: space-between; align-items: center; border-left: 4px solid #FF9800; }
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
  `]
})
export class MandiComponent implements OnInit {
  activeTab: 'prices' | 'fertilizers' = 'prices';

  // --- Mandi Prices State ---
  commodities: string[] = [];
  selectedCommodity: string = 'WHEAT'; // Default
  prices: MandiPriceDto[] = [];
  isLoadingPrices = false;
  errorMessagePrices = '';

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
    this.loadCommodities();

    // Auto-fetch defaults
    setTimeout(() => this.fetchPrices(), 500);
    this.detectLocationForFertilizers();
  }

  switchTab(tab: 'prices' | 'fertilizers') {
    this.activeTab = tab;
  }

  // ============== PRICES LOGIC ==============

  loadCommodities() {
    this.mandiService.getCommodities().subscribe({
      next: (data) => {
        this.commodities = data;
        if (!this.commodities.includes(this.selectedCommodity) && this.commodities.length > 0) {
          this.selectedCommodity = this.commodities[0];
        }
      },
      error: (err) => {
        console.error('Failed to load commodities', err);
        // Fallback hardcoded lists just in case API is empty during testing
        this.commodities = ['WHEAT', 'RICE', 'COTTON', 'SUGARCANE', 'ONION', 'POTATO'];
      }
    });
  }

  fetchPrices() {
    if (!this.selectedCommodity) return;

    this.isLoadingPrices = true;
    this.errorMessagePrices = '';
    this.prices = [];

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.mandiService.getNearbyPrices(
            this.selectedCommodity,
            position.coords.latitude,
            position.coords.longitude,
            100 // 100km radius
          ).subscribe({
            next: (data: any) => {
              this.prices = Array.isArray(data) ? data : [data];
              this.isLoadingPrices = false;
            },
            error: () => this.fallbackToGeneralPrices()
          });
        },
        () => this.fallbackToGeneralPrices()
      );
    } else {
      this.fallbackToGeneralPrices();
    }
  }

  private fallbackToGeneralPrices() {
    this.mandiService.getPrices(this.selectedCommodity).subscribe({
      next: (data: any) => {
        this.prices = Array.isArray(data) ? data : [data];
        this.isLoadingPrices = false;
      },
      error: (err) => {
        this.errorMessagePrices = 'Failed to load mandi prices. Please ensure the backend is running.';
        this.isLoadingPrices = false;
      }
    });
  }

  // ============== FERTILIZER LOGIC ==============

  detectLocationForFertilizers() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(async (position) => {
        try {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          const response: any = await firstValueFrom(
            this.http.get(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=en`)
          );
          if (response) {
            this.filterState = response.principalSubdivision || 'Maharashtra';
            this.filterDistrict = response.city || response.locality || 'Pune';
            this.fetchFertilizers(); // Auto-fetch if reverse geocode succeeds
          }
        } catch (e) {
          console.warn('Reverse geocode for fertilizers failed', e);
        }
      });
    }
  }

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
        console.error('Fertilizer fetch error:', err);
        // Fallback to fetch all and filter client side if location endpoint is picky about case
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
