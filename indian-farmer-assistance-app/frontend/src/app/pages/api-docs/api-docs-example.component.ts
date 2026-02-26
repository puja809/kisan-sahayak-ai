import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiDocumentationService, ServiceDocumentation } from '../../services/api-documentation.service';
import { ApiClientGeneratorService, GeneratedClient } from '../../services/api-client-generator.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Example component demonstrating API documentation service usage
 * This component shows various ways to interact with the API documentation
 */
@Component({
  selector: 'app-api-docs-example',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="example-container">
      <h1>API Documentation Examples</h1>

      <!-- Example 1: Service Selection -->
      <section class="example-section">
        <h2>Example 1: Browse Services</h2>
        <div class="service-selector">
          <select [(ngModel)]="selectedServiceId" (change)="loadServiceDocumentation()">
            <option *ngFor="let service of services" [value]="service.id">
              {{ service.name }}
            </option>
          </select>
        </div>

        <div *ngIf="currentDocumentation" class="documentation-display">
          <h3>{{ currentDocumentation.serviceName }}</h3>
          <p>Version: {{ currentDocumentation.version }}</p>
          <p>Port: {{ currentDocumentation.port }}</p>
          <p>Endpoints: {{ currentDocumentation.endpoints.length }}</p>

          <div class="endpoints-preview">
            <h4>Sample Endpoints:</h4>
            <div *ngFor="let endpoint of currentDocumentation.endpoints | slice:0:5" class="endpoint">
              <span class="method" [ngClass]="endpoint.method.toLowerCase()">{{ endpoint.method }}</span>
              <span class="path">{{ endpoint.path }}</span>
              <p *ngIf="endpoint.summary">{{ endpoint.summary }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Example 2: Search Endpoints -->
      <section class="example-section">
        <h2>Example 2: Search Endpoints</h2>
        <div class="search-box">
          <input 
            type="text" 
            [(ngModel)]="searchQuery" 
            placeholder="Search endpoints (e.g., 'user', 'crop', 'weather')"
            (keyup)="searchEndpoints()">
        </div>

        <div *ngIf="searchResults.length > 0" class="search-results">
          <div *ngFor="let result of searchResults" class="search-result">
            <h4>{{ result.service.name }}</h4>
            <div *ngFor="let endpoint of result.endpoints" class="endpoint">
              <span class="method" [ngClass]="endpoint.method.toLowerCase()">{{ endpoint.method }}</span>
              <span class="path">{{ endpoint.path }}</span>
            </div>
          </div>
        </div>

        <div *ngIf="searchQuery && searchResults.length === 0" class="no-results">
          No endpoints found for "{{ searchQuery }}"
        </div>
      </section>

      <!-- Example 3: Generate Client Code -->
      <section class="example-section">
        <h2>Example 3: Generate Client Code</h2>
        <div class="generator-controls">
          <select [(ngModel)]="generatorServiceId">
            <option *ngFor="let service of services" [value]="service.id">
              {{ service.name }}
            </option>
          </select>
          <button (click)="generateClientCode()">Generate Client</button>
        </div>

        <div *ngIf="generatedClient" class="generated-code">
          <h4>Generated: {{ generatedClient.className }}</h4>
          <pre><code>{{ generatedClient.code }}</code></pre>
          <button (click)="downloadGeneratedCode()">Download as File</button>
        </div>
      </section>

      <!-- Example 4: Load All Services -->
      <section class="example-section">
        <h2>Example 4: Load All Services</h2>
        <button (click)="loadAllServices()">Load All Services</button>

        <div *ngIf="allServices.length > 0" class="all-services">
          <div *ngFor="let service of allServices" class="service-summary">
            <h4>{{ service.serviceName }}</h4>
            <p>Endpoints: {{ service.endpoints.length }}</p>
            <p>Port: {{ service.port }}</p>
          </div>
        </div>
      </section>

      <!-- Example 5: Service Information -->
      <section class="example-section">
        <h2>Example 5: Service Information</h2>
        <div class="services-info">
          <div *ngFor="let service of services" class="service-info">
            <h4>{{ service.name }}</h4>
            <p>{{ service.description }}</p>
            <p class="port">Port: {{ service.port }}</p>
            <div class="links">
              <a [href]="getSwaggerUrl(service.id)" target="_blank">Swagger UI</a>
              <a [href]="getOpenApiUrl(service.id)" target="_blank">OpenAPI JSON</a>
            </div>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .example-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
      font-family: Arial, sans-serif;
    }

    h1 {
      color: #333;
      border-bottom: 3px solid #2196F3;
      padding-bottom: 10px;
    }

    .example-section {
      background: #f9f9f9;
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
    }

    .example-section h2 {
      color: #2196F3;
      margin-top: 0;
    }

    .service-selector, .search-box, .generator-controls {
      display: flex;
      gap: 10px;
      margin-bottom: 15px;
    }

    select, input, button {
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    select, input {
      flex: 1;
    }

    button {
      background: #2196F3;
      color: white;
      cursor: pointer;
      border: none;
      font-weight: bold;
      transition: background 0.3s;
    }

    button:hover {
      background: #1976D2;
    }

    .documentation-display {
      background: white;
      padding: 15px;
      border-radius: 4px;
      margin-top: 15px;
    }

    .endpoints-preview {
      margin-top: 15px;
    }

    .endpoint {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px;
      background: #f5f5f5;
      border-radius: 4px;
      margin-bottom: 8px;
      border-left: 4px solid #2196F3;
    }

    .method {
      display: inline-block;
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: bold;
      font-size: 0.85rem;
      color: white;
      min-width: 50px;
      text-align: center;
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

    .path {
      font-family: 'Courier New', monospace;
      color: #333;
      font-weight: 500;
      flex: 1;
    }

    .search-results {
      margin-top: 15px;
    }

    .search-result {
      background: white;
      padding: 15px;
      border-radius: 4px;
      margin-bottom: 10px;
    }

    .no-results {
      background: #fff3cd;
      border: 1px solid #ffc107;
      color: #856404;
      padding: 15px;
      border-radius: 4px;
      margin-top: 15px;
    }

    .generated-code {
      background: white;
      padding: 15px;
      border-radius: 4px;
      margin-top: 15px;
    }

    pre {
      background: #f5f5f5;
      padding: 15px;
      border-radius: 4px;
      overflow-x: auto;
      max-height: 400px;
      overflow-y: auto;
    }

    code {
      font-family: 'Courier New', monospace;
      font-size: 0.9rem;
    }

    .all-services {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 15px;
      margin-top: 15px;
    }

    .service-summary {
      background: white;
      padding: 15px;
      border-radius: 4px;
      border-left: 4px solid #2196F3;
    }

    .services-info {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 15px;
      margin-top: 15px;
    }

    .service-info {
      background: white;
      padding: 15px;
      border-radius: 4px;
      border: 1px solid #ddd;
    }

    .port {
      font-family: 'Courier New', monospace;
      color: #666;
      font-size: 0.9rem;
    }

    .links {
      display: flex;
      gap: 10px;
      margin-top: 10px;
    }

    .links a {
      color: #2196F3;
      text-decoration: none;
      font-size: 0.9rem;
      padding: 5px 10px;
      border: 1px solid #2196F3;
      border-radius: 4px;
      transition: all 0.3s;
    }

    .links a:hover {
      background: #2196F3;
      color: white;
    }
  `]
})
export class ApiDocsExampleComponent implements OnInit, OnDestroy {
  services: any[] = [];
  selectedServiceId = 'user-service';
  generatorServiceId = 'user-service';
  searchQuery = '';
  currentDocumentation: ServiceDocumentation | null = null;
  searchResults: any[] = [];
  generatedClient: GeneratedClient | null = null;
  allServices: ServiceDocumentation[] = [];
  private destroy$ = new Subject<void>();

  constructor(
    private apiDocService: ApiDocumentationService,
    private clientGenerator: ApiClientGeneratorService
  ) {
    this.services = this.apiDocService.getServices();
  }

  ngOnInit(): void {
    this.loadServiceDocumentation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadServiceDocumentation(): void {
    this.apiDocService.fetchServiceDocumentation(this.selectedServiceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(doc => {
        this.currentDocumentation = doc;
      });
  }

  searchEndpoints(): void {
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

  generateClientCode(): void {
    this.apiDocService.fetchServiceDocumentation(this.generatorServiceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(doc => {
        this.generatedClient = this.clientGenerator.generateClient(doc);
      });
  }

  downloadGeneratedCode(): void {
    if (this.generatedClient) {
      this.clientGenerator.exportAsFile(this.generatedClient);
    }
  }

  loadAllServices(): void {
    this.apiDocService.fetchAllServiceDocumentation()
      .pipe(takeUntil(this.destroy$))
      .subscribe(docs => {
        this.allServices = docs;
      });
  }

  getSwaggerUrl(serviceId: string): string {
    return this.apiDocService.getSwaggerUrl(serviceId);
  }

  getOpenApiUrl(serviceId: string): string {
    return this.apiDocService.getOpenApiJsonUrl(serviceId);
  }
}
