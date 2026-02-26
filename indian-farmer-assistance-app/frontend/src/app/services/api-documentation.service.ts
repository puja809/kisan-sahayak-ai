import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { catchError, tap, shareReplay } from 'rxjs/operators';

export interface ServiceEndpoint {
  path: string;
  method: string;
  summary?: string;
  description?: string;
  parameters?: any[];
  requestBody?: any;
  responses?: any;
}

export interface ServiceDocumentation {
  serviceId: string;
  serviceName: string;
  port: number;
  version?: string;
  title?: string;
  description?: string;
  endpoints: ServiceEndpoint[];
  rawSpec?: any;
}

export interface ApiService {
  id: string;
  name: string;
  description: string;
  port: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiDocumentationService {
  private readonly services: ApiService[] = [
    { id: 'user-service', name: 'User Service', description: 'User authentication, profile management, and AgriStack integration', port: 8099 },
    { id: 'weather-service', name: 'Weather Service', description: 'IMD weather forecasts, alerts, and agromet advisories', port: 8100 },
    { id: 'crop-service', name: 'Crop Service', description: 'Crop recommendations, rotation planning, and yield estimation', port: 8093 },
    { id: 'scheme-service', name: 'Scheme Service', description: 'Government schemes, eligibility assessment, and applications', port: 8097 },
    { id: 'mandi-service', name: 'Mandi Service', description: 'AGMARKNET price data, trends, and alerts', port: 8096 },
    { id: 'location-service', name: 'Location Service', description: 'GPS services, reverse geocoding, and government body locator', port: 8095 },
    { id: 'iot-service', name: 'IoT Service', description: 'IoT device management and sensor data collection', port: 8094 },
    { id: 'admin-service', name: 'Admin Service', description: 'Document management and system administration', port: 8091 }
  ];

  private documentationCache = new Map<string, Observable<ServiceDocumentation>>();
  private loadingState = new BehaviorSubject<{ [key: string]: boolean }>({});

  constructor(private http: HttpClient) {}

  getServices(): ApiService[] {
    return this.services;
  }

  getServiceById(serviceId: string): ApiService | undefined {
    return this.services.find(s => s.id === serviceId);
  }

  getServicePort(serviceId: string): number {
    return this.getServiceById(serviceId)?.port || 8080;
  }

  getSwaggerUrl(serviceId: string): string {
    const port = this.getServicePort(serviceId);
    return `http://localhost:${port}/swagger-ui.html`;
  }

  getOpenApiJsonUrl(serviceId: string): string {
    const port = this.getServicePort(serviceId);
    return `http://localhost:${port}/v3/api-docs`;
  }

  getOpenApiYamlUrl(serviceId: string): string {
    const port = this.getServicePort(serviceId);
    return `http://localhost:${port}/v3/api-docs.yaml`;
  }

  /**
   * Fetch OpenAPI documentation for a service
   */
  fetchServiceDocumentation(serviceId: string): Observable<ServiceDocumentation> {
    // Return cached observable if available
    if (this.documentationCache.has(serviceId)) {
      return this.documentationCache.get(serviceId)!;
    }

    const service = this.getServiceById(serviceId);
    if (!service) {
      return of({
        serviceId,
        serviceName: 'Unknown',
        port: 8080,
        endpoints: []
      });
    }

    // Mark as loading
    const loadingState = this.loadingState.value;
    loadingState[serviceId] = true;
    this.loadingState.next(loadingState);

    const url = this.getOpenApiJsonUrl(serviceId);
    const observable = this.http.get<any>(url).pipe(
      tap(spec => {
        const documentation = this.parseOpenApiSpec(spec, service);
        // Mark as not loading
        const state = this.loadingState.value;
        state[serviceId] = false;
        this.loadingState.next(state);
      }),
      catchError(error => {
        console.error(`Failed to fetch documentation for ${serviceId}:`, error);
        // Mark as not loading
        const state = this.loadingState.value;
        state[serviceId] = false;
        this.loadingState.next(state);
        return of({
          serviceId,
          serviceName: service.name,
          port: service.port,
          endpoints: [],
          rawSpec: null
        });
      }),
      shareReplay(1)
    );

    this.documentationCache.set(serviceId, observable);
    return observable;
  }

  /**
   * Parse OpenAPI 3.0 specification into structured documentation
   */
  private parseOpenApiSpec(spec: any, service: ApiService): ServiceDocumentation {
    const endpoints: ServiceEndpoint[] = [];

    if (spec.paths) {
      Object.entries(spec.paths).forEach(([path, pathItem]: [string, any]) => {
        Object.entries(pathItem).forEach(([method, operation]: [string, any]) => {
          if (['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(method.toLowerCase())) {
            endpoints.push({
              path,
              method: method.toUpperCase(),
              summary: operation.summary,
              description: operation.description,
              parameters: operation.parameters,
              requestBody: operation.requestBody,
              responses: operation.responses
            });
          }
        });
      });
    }

    return {
      serviceId: service.id,
      serviceName: service.name,
      port: service.port,
      version: spec.info?.version,
      title: spec.info?.title,
      description: spec.info?.description,
      endpoints,
      rawSpec: spec
    };
  }

  /**
   * Get all service documentation
   */
  fetchAllServiceDocumentation(): Observable<ServiceDocumentation[]> {
    const observables = this.services.map(service =>
      this.fetchServiceDocumentation(service.id)
    );
    return new Observable(subscriber => {
      const results: ServiceDocumentation[] = [];
      let completed = 0;

      observables.forEach((obs, index) => {
        obs.subscribe(
          doc => {
            results[index] = doc;
            completed++;
            if (completed === observables.length) {
              subscriber.next(results);
              subscriber.complete();
            }
          },
          error => {
            results[index] = {
              serviceId: this.services[index].id,
              serviceName: this.services[index].name,
              port: this.services[index].port,
              endpoints: []
            };
            completed++;
            if (completed === observables.length) {
              subscriber.next(results);
              subscriber.complete();
            }
          }
        );
      });
    });
  }

  /**
   * Search endpoints across all services
   */
  searchEndpoints(query: string): Observable<{ service: ApiService; endpoints: ServiceEndpoint[] }[]> {
    return new Observable(subscriber => {
      const results: { service: ApiService; endpoints: ServiceEndpoint[] }[] = [];
      let completed = 0;

      this.services.forEach((service, index) => {
        this.fetchServiceDocumentation(service.id).subscribe(
          doc => {
            const filtered = doc.endpoints.filter(ep =>
              ep.path.toLowerCase().includes(query.toLowerCase()) ||
              ep.summary?.toLowerCase().includes(query.toLowerCase()) ||
              ep.description?.toLowerCase().includes(query.toLowerCase())
            );

            if (filtered.length > 0) {
              results.push({ service, endpoints: filtered });
            }

            completed++;
            if (completed === this.services.length) {
              subscriber.next(results);
              subscriber.complete();
            }
          },
          () => {
            completed++;
            if (completed === this.services.length) {
              subscriber.next(results);
              subscriber.complete();
            }
          }
        );
      });
    });
  }

  isLoading(serviceId: string): Observable<boolean> {
    return new Observable(subscriber => {
      this.loadingState.subscribe(state => {
        subscriber.next(state[serviceId] || false);
      });
    });
  }

  clearCache(): void {
    this.documentationCache.clear();
  }
}
