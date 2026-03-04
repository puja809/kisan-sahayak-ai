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
  templateUrl: './api-docs.component.html',
  styleUrls: ['./api-docs.component.css'],})
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
