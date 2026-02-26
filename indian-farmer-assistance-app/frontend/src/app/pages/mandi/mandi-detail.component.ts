import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

interface MandiPrice {
  id: number;
  commodityName: string;
  variety: string;
  mandiName: string;
  state: string;
  district: string;
  modalPrice: number;
  minPrice: number;
  maxPrice: number;
  arrivalQuantity: number;
  priceDate: string;
}

interface PriceTrend {
  date: string;
  price: number;
}

interface Mandi {
  id: number;
  mandiName: string;
  state: string;
  district: string;
  distance: number;
  operatingHours: string;
  contactInfo: string;
}

interface PriceAlert {
  id: number;
  commodityName: string;
  targetPrice: number;
  isActive: boolean;
}

@Component({
  selector: 'app-mandi-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mandi-container">
      <div class="header">
        <h1>Mandi Prices</h1>
      </div>

      <div class="mandi-grid">
        <!-- Price Search -->
        <div class="card search-card">
          <h2>Search Prices</h2>
          <div class="search-form">
            <input type="text" [(ngModel)]="searchCommodity" placeholder="Enter commodity name" />
            <button class="btn-primary" (click)="searchPrices()">Search</button>
          </div>

          <div *ngIf="searchResults.length > 0" class="search-results">
            <div *ngFor="let result of searchResults" class="price-item">
              <h3>{{ result.commodityName }} ({{ result.variety }})</h3>
              <p>Mandi: {{ result.mandiName }}, {{ result.district }}</p>
              <p>Modal Price: ₹{{ result.modalPrice }}/quintal</p>
              <p>Range: ₹{{ result.minPrice }} - ₹{{ result.maxPrice }}</p>
              <p>Arrival: {{ result.arrivalQuantity }} quintals</p>
              <p class="date">{{ result.priceDate | date: 'short' }}</p>
            </div>
          </div>
          <p *ngIf="searchResults.length === 0 && searched" class="no-data">No prices found</p>
        </div>

        <!-- Nearby Mandis -->
        <div class="card mandis-card">
          <h2>Nearby Mandis</h2>
          <div *ngIf="nearbyMandis.length > 0" class="mandis-list">
            <div *ngFor="let mandi of nearbyMandis" class="mandi-item">
              <h3>{{ mandi.mandiName }}</h3>
              <p>Distance: {{ mandi.distance }} km</p>
              <p>Location: {{ mandi.district }}, {{ mandi.state }}</p>
              <p>Hours: {{ mandi.operatingHours }}</p>
              <p>Contact: {{ mandi.contactInfo }}</p>
              <button class="btn-secondary" (click)="getDirections(mandi)">Get Directions</button>
            </div>
          </div>
          <p *ngIf="nearbyMandis.length === 0" class="no-data">No nearby mandis found</p>
        </div>

        <!-- Price Trends -->
        <div class="card trends-card">
          <h2>30-Day Price Trends</h2>
          <div *ngIf="priceTrends.length > 0" class="trends-info">
            <p>Commodity: {{ selectedCommodity }}</p>
            <p>Current Price: ₹{{ currentPrice }}/quintal</p>
            <p>30-Day Avg: ₹{{ getAveragePrice() }}/quintal</p>
            <p>Min: ₹{{ getMinPrice() }} | Max: ₹{{ getMaxPrice() }}</p>
            <p class="chart-placeholder">Chart visualization would be displayed here</p>
          </div>
          <p *ngIf="priceTrends.length === 0" class="no-data">No trend data available</p>
        </div>

        <!-- MSP Comparison -->
        <div class="card msp-card">
          <h2>MSP Comparison</h2>
          <div class="msp-info">
            <p>MSP (Minimum Support Price) comparison with current market prices</p>
            <div class="msp-item">
              <p>Commodity: {{ selectedCommodity }}</p>
              <p>MSP: ₹{{ mspPrice }}/quintal</p>
              <p>Market Price: ₹{{ currentPrice }}/quintal</p>
              <p [class]="currentPrice >= mspPrice ? 'above-msp' : 'below-msp'">
                {{ currentPrice >= mspPrice ? '✓ Above MSP' : '✗ Below MSP' }}
              </p>
            </div>
          </div>
        </div>

        <!-- Price Alerts -->
        <div class="card alerts-card">
          <h2>Price Alerts</h2>
          <div class="alert-form">
            <input type="text" [(ngModel)]="alertCommodity" placeholder="Commodity name" />
            <input type="number" [(ngModel)]="alertPrice" placeholder="Target price" />
            <button class="btn-primary" (click)="subscribeAlert()">Subscribe Alert</button>
          </div>

          <div *ngIf="myAlerts.length > 0" class="alerts-list">
            <div *ngFor="let alert of myAlerts" class="alert-item">
              <p>{{ alert.commodityName }}: ₹{{ alert.targetPrice }}/quintal</p>
              <p class="status" [class]="alert.isActive ? 'active' : 'inactive'">
                {{ alert.isActive ? 'Active' : 'Inactive' }}
              </p>
              <button class="btn-small" (click)="removeAlert(alert)">Remove</button>
            </div>
          </div>
          <p *ngIf="myAlerts.length === 0" class="no-data">No alerts set</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mandi-container {
      padding: 1.5rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      margin-bottom: 2rem;
    }

    .header h1 {
      font-size: 2rem;
      color: #333;
      margin: 0;
    }

    .mandi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
    }

    .card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .card h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .search-form,
    .alert-form {
      display: flex;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .search-form input,
    .alert-form input {
      flex: 1;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .search-form input:focus,
    .alert-form input:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .btn-primary {
      padding: 0.75rem 1.5rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }

    .btn-primary:hover {
      background: #5568d3;
    }

    .search-results,
    .mandis-list,
    .alerts-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .price-item,
    .mandi-item,
    .alert-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .price-item h3,
    .mandi-item h3 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .price-item p,
    .mandi-item p,
    .alert-item p {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .date {
      font-size: 0.85rem;
      color: #999;
    }

    .btn-secondary {
      width: 100%;
      padding: 0.75rem;
      background: #6c757d;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
      margin-top: 1rem;
    }

    .btn-secondary:hover {
      background: #5a6268;
    }

    .btn-small {
      padding: 0.5rem 1rem;
      background: #e74c3c;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      margin-top: 0.5rem;
    }

    .btn-small:hover {
      background: #c0392b;
    }

    .trends-info,
    .msp-info {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
    }

    .trends-info p,
    .msp-info p {
      margin: 0.5rem 0;
      color: #666;
    }

    .msp-item {
      margin-top: 1rem;
      padding: 1rem;
      background: white;
      border-radius: 4px;
      border-left: 4px solid #667eea;
    }

    .above-msp {
      color: #27ae60;
      font-weight: 600;
    }

    .below-msp {
      color: #e74c3c;
      font-weight: 600;
    }

    .status {
      font-weight: 600;
    }

    .status.active {
      color: #27ae60;
    }

    .status.inactive {
      color: #e74c3c;
    }

    .chart-placeholder {
      text-align: center;
      color: #999;
      padding: 2rem;
      background: white;
      border-radius: 4px;
      margin-top: 1rem;
    }

    .no-data {
      color: #999;
      font-style: italic;
    }

    @media (max-width: 768px) {
      .mandi-grid {
        grid-template-columns: 1fr;
      }

      .search-form,
      .alert-form {
        flex-direction: column;
      }
    }
  `]
})
export class MandiDetailComponent implements OnInit {
  searchCommodity = '';
  searchResults: MandiPrice[] = [];
  nearbyMandis: Mandi[] = [];
  priceTrends: PriceTrend[] = [];
  myAlerts: PriceAlert[] = [];
  alertCommodity = '';
  alertPrice: number | null = null;
  selectedCommodity = '';
  currentPrice = 0;
  mspPrice = 0;
  searched = false;

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadNearbyMandis();
    this.loadMyAlerts();
  }

  searchPrices(): void {
    if (!this.searchCommodity) return;

    this.searched = true;
    this.http.get<MandiPrice[]>(`/api/v1/mandi/prices/${this.searchCommodity}`).subscribe({
      next: (prices) => {
        this.searchResults = prices;
        if (prices.length > 0) {
          this.selectedCommodity = prices[0].commodityName;
          this.currentPrice = prices[0].modalPrice;
          this.loadPriceTrends();
          this.loadMSPComparison();
        }
      },
      error: (error) => this.toastr.error('Failed to search prices')
    });
  }

  private loadNearbyMandis(): void {
    this.http.get<Mandi[]>('/api/v1/mandi/locations/nearby').subscribe({
      next: (mandis) => this.nearbyMandis = mandis,
      error: (error) => console.error('Failed to load nearby mandis:', error)
    });
  }

  private loadPriceTrends(): void {
    this.http.get<PriceTrend[]>(`/api/v1/mandi/prices/trends/${this.selectedCommodity}`).subscribe({
      next: (trends) => this.priceTrends = trends,
      error: (error) => console.error('Failed to load price trends:', error)
    });
  }

  private loadMSPComparison(): void {
    this.http.get<any>(`/api/v1/mandi/prices/msp/${this.selectedCommodity}`).subscribe({
      next: (data) => this.mspPrice = data.mspPrice,
      error: (error) => console.error('Failed to load MSP:', error)
    });
  }

  private loadMyAlerts(): void {
    this.http.get<PriceAlert[]>('/api/v1/mandi/alerts').subscribe({
      next: (alerts) => this.myAlerts = alerts,
      error: (error) => console.error('Failed to load alerts:', error)
    });
  }

  subscribeAlert(): void {
    if (!this.alertCommodity || !this.alertPrice) return;

    this.http.post('/api/v1/mandi/alerts/subscribe', {
      commodityName: this.alertCommodity,
      targetPrice: this.alertPrice
    }).subscribe({
      next: () => {
        this.toastr.success('Alert subscribed');
        this.alertCommodity = '';
        this.alertPrice = null;
        this.loadMyAlerts();
      },
      error: (error) => this.toastr.error('Failed to subscribe alert')
    });
  }

  removeAlert(alert: PriceAlert): void {
    this.http.delete(`/api/v1/mandi/alerts/${alert.id}`).subscribe({
      next: () => {
        this.toastr.success('Alert removed');
        this.loadMyAlerts();
      },
      error: (error) => this.toastr.error('Failed to remove alert')
    });
  }

  getDirections(mandi: Mandi): void {
    console.log('Get directions to:', mandi);
  }

  getAveragePrice(): number {
    if (this.priceTrends.length === 0) return 0;
    const sum = this.priceTrends.reduce((acc, trend) => acc + trend.price, 0);
    return Math.round(sum / this.priceTrends.length);
  }

  getMinPrice(): number {
    if (this.priceTrends.length === 0) return 0;
    return Math.min(...this.priceTrends.map(t => t.price));
  }

  getMaxPrice(): number {
    if (this.priceTrends.length === 0) return 0;
    return Math.max(...this.priceTrends.map(t => t.price));
  }
}
