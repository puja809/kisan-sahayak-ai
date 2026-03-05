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
  templateUrl: './api-docs-example.component.html',
  styleUrls: ['./api-docs-example.component.css'],})
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
