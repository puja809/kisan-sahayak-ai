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
  templateUrl: './mandi-detail.component.html',
  styleUrls: ['./mandi-detail.component.css'],})
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
