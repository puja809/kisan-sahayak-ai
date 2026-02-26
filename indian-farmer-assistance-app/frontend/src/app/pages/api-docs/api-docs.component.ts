import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiDocumentationService, ServiceDocumentation, ServiceEndpoint } from '../../services/api-documentation.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * API Documentation Component
 * Displays Swagger/OpenAPI documentation for all backend services
 */
@Component({
  selector: 'app-api-docs',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  template: `
    <div class="api-docs-container">
      <div class="docs-header">
        <h1>API Documentation</h1>
        <p>Indian Farmer Assistance Application - REST API Reference</p>
      </div>

      <div class="docs-content">
        <!-- Search Bar -->
        <div class="search-section">
          <input 
            type="text" 
            placeholder="Search endpoints..." 
            [(ngModel)]="searchQuery"
            (keyup)="onSearch()"
            class="search-input">
          <button (click)="clearSearch()" class="clear-btn" *ngIf="searchQuery">Clear</button>
        </div>

        <!-- Service Tabs -->
        <div class="service-tabs">
          <button 
            *ngFor="let service of services" 
            [class.active]="selectedService === service.id"
            (click)="selectService(service.id)"
            class="service-tab">
            {{ service.name }}
            <span class="loading-indicator" *ngIf="(isLoading$ | async)">⟳</span>
          </button>
        </div>

        <!-- Swagger UI Iframe -->
        <div class="swagger-ui-wrapper" *ngIf="selectedService && !searchQuery">
          <iframe 
            [src]="getSwaggerUrl(selectedService)" 
            class="swagger-iframe"
            title="Swagger UI">
          </iframe>
        </div>

        <!-- Endpoints List (when searching) -->
        <div class="endpoints-list" *ngIf="searchQuery && searchResults.length > 0">
          <div *ngFor="let result of searchResults" class="service-endpoints">
            <h3>{{ result.service.name }}</h3>
            <div class="endpoints">
              <div *ngFor="let endpoint of result.endpoints" class="endpoint-item">
                <div class="endpoint-header">
                  <span class="method" [ngClass]="endpoint.method.toLowerCase()">{{ endpoint.method }}</span>
                  <span class="path">{{ endpoint.path }}</span>
                </div>
                <div class="endpoint-details" *ngIf="endpoint.summary || endpoint.description">
                  <p class="summary">{{ endpoint.summary }}</p>
                  <p class="description" *ngIf="endpoint.description">{{ endpoint.description }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="no-results" *ngIf="searchQuery && searchResults.length === 0">
          <p>No endpoints found matching "{{ searchQuery }}"</p>
        </div>

        <!-- Service Cards -->
        <div class="api-info" *ngIf="!searchQuery">
          <h2>Available Services</h2>
          <div class="services-grid">
            <div *ngFor="let service of services" class="service-card">
              <h3>{{ service.name }}</h3>
              <p>{{ service.description }}</p>
              <div class="service-meta">
                <span class="port">Port: {{ service.port }}</span>
              </div>
              <div class="service-links">
                <a [href]="getSwaggerUrl(service.id)" target="_blank" class="docs-link">
                  Swagger UI →
                </a>
                <a [href]="getOpenApiJsonUrl(service.id)" target="_blank" class="docs-link">
                  OpenAPI JSON →
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .api-docs-container {
      padding: 20px;
      max-width: 1400px;
      margin: 0 auto;
    }

    .docs-header {
      text-align: center;
      margin-bottom: 40px;
      border-bottom: 2px solid #e0e0e0;
      padding-bottom: 20px;
    }

    .docs-header h1 {
      font-size: 2.5rem;
      color: #333;
      margin: 0;
    }

    .docs-header p {
      color: #666;
      font-size: 1.1rem;
      margin: 10px 0 0 0;
    }

    .search-section {
      display: flex;
      gap: 10px;
      margin-bottom: 20px;
    }

    .search-input {
      flex: 1;
      padding: 12px 16px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
      transition: border-color 0.3s ease;
    }

    .search-input:focus {
      outline: none;
      border-color: #2196F3;
      box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
    }

    .clear-btn {
      padding: 12px 20px;
      background: #f44336;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
      transition: background 0.3s ease;
    }

    .clear-btn:hover {
      background: #d32f2f;
    }

    .service-tabs {
      display: flex;
      gap: 10px;
      margin-bottom: 20px;
      flex-wrap: wrap;
      border-bottom: 1px solid #ddd;
    }

    .service-tab {
      padding: 10px 20px;
      border: none;
      background: #f5f5f5;
      cursor: pointer;
      border-radius: 4px 4px 0 0;
      font-weight: 500;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .service-tab:hover {
      background: #e8e8e8;
    }

    .service-tab.active {
      background: #2196F3;
      color: white;
    }

    .loading-indicator {
      display: inline-block;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .swagger-ui-wrapper {
      background: white;
      border: 1px solid #ddd;
      border-radius: 4px;
      margin-bottom: 40px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .swagger-iframe {
      width: 100%;
      height: 800px;
      border: none;
      border-radius: 4px;
    }

    .endpoints-list {
      margin-bottom: 40px;
    }

    .service-endpoints {
      background: white;
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .service-endpoints h3 {
      margin: 0 0 15px 0;
      color: #2196F3;
      font-size: 1.3rem;
      border-bottom: 2px solid #e0e0e0;
      padding-bottom: 10px;
    }

    .endpoints {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .endpoint-item {
      border-left: 4px solid #2196F3;
      padding-left: 12px;
      padding-top: 8px;
      padding-bottom: 8px;
    }

    .endpoint-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }

    .method {
      display: inline-block;
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: bold;
      font-size: 0.85rem;
      min-width: 50px;
      text-align: center;
      color: white;
    }

    .method.get {
      background: #4CAF50;
    }

    .method.post {
      background: #2196F3;
    }

    .method.put {
      background: #FF9800;
    }

    .method.delete {
      background: #f44336;
    }

    .method.patch {
      background: #9C27B0;
    }

    .path {
      font-family: 'Courier New', monospace;
      color: #333;
      font-weight: 500;
      flex: 1;
      word-break: break-all;
    }

    .endpoint-details {
      margin-top: 8px;
    }

    .summary {
      margin: 0;
      color: #555;
      font-weight: 500;
    }

    .description {
      margin: 4px 0 0 0;
      color: #888;
      font-size: 0.95rem;
      line-height: 1.4;
    }

    .no-results {
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      padding: 20px;
      text-align: center;
      color: #856404;
    }

    .api-info {
      margin-top: 40px;
    }

    .api-info h2 {
      font-size: 1.8rem;
      color: #333;
      margin-bottom: 20px;
    }

    .services-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 20px;
    }

    .service-card {
      background: white;
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      transition: transform 0.3s ease, box-shadow 0.3s ease;
      display: flex;
      flex-direction: column;
    }

    .service-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }

    .service-card h3 {
      margin: 0 0 10px 0;
      color: #2196F3;
      font-size: 1.3rem;
    }

    .service-card p {
      color: #666;
      margin: 0 0 15px 0;
      line-height: 1.5;
      flex: 1;
    }

    .service-meta {
      margin-bottom: 15px;
      padding-bottom: 15px;
      border-bottom: 1px solid #eee;
    }

    .port {
      display: inline-block;
      background: #f5f5f5;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.9rem;
      color: #666;
      font-family: 'Courier New', monospace;
    }

    .service-links {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .docs-link {
      display: inline-block;
      color: #2196F3;
      text-decoration: none;
      font-weight: 500;
      transition: color 0.3s ease;
      padding: 8px 12px;
      border: 1px solid #2196F3;
      border-radius: 4px;
      font-size: 0.9rem;
    }

    .docs-link:hover {
      color: white;
      background: #2196F3;
    }

    @media (max-width: 768px) {
      .api-docs-container {
        padding: 10px;
      }

      .docs-header h1 {
        font-size: 1.8rem;
      }

      .swagger-iframe {
        height: 600px;
      }

      .services-grid {
        grid-template-columns: 1fr;
      }

      .search-section {
        flex-direction: column;
      }

      .endpoint-header {
        flex-direction: column;
        align-items: flex-start;
      }
    }
  `]
})
export class ApiDocsComponent implements OnInit, OnDestroy {
  selectedService: string = 'user-service';
  searchQuery: string = '';
  searchResults: any[] = [];
  isLoading$ = new Subject<boolean>();
  private destroy$ = new Subject<void>();
  private sanitizer = inject(DomSanitizer);

  services: any[] = [];

  constructor(private apiDocService: ApiDocumentationService) {
    this.services = this.apiDocService.getServices();
  }

  ngOnInit(): void {
    this.selectedService = this.services[0].id;
    this.updateLoadingState();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selectService(serviceId: string): void {
    this.selectedService = serviceId;
    this.updateLoadingState();
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.apiDocService.searchEndpoints(this.searchQuery)
        .pipe(takeUntil(this.destroy$))
        .subscribe(results => {
          this.searchResults = results;
        });
    } else {
      this.searchResults = [];
    }
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
  }

  getSwaggerUrl(serviceId: string): SafeResourceUrl {
    const url = this.apiDocService.getSwaggerUrl(serviceId);
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  getOpenApiJsonUrl(serviceId: string): string {
    return this.apiDocService.getOpenApiJsonUrl(serviceId);
  }

  private updateLoadingState(): void {
    this.apiDocService.isLoading(this.selectedService)
      .pipe(takeUntil(this.destroy$))
      .subscribe(loading => {
        (this.isLoading$ as Subject<boolean>).next(loading);
      });
  }
}
