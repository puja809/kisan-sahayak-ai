import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

/**
 * API Documentation Component
 * Displays Swagger/OpenAPI documentation for all backend services
 */
@Component({
  selector: 'app-api-docs',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  template: `
    <div class="api-docs-container">
      <div class="docs-header">
        <h1>API Documentation</h1>
        <p>Indian Farmer Assistance Application - REST API Reference</p>
      </div>

      <div class="docs-content">
        <div class="service-tabs">
          <button 
            *ngFor="let service of services" 
            [class.active]="selectedService === service.id"
            (click)="selectService(service.id)"
            class="service-tab">
            {{ service.name }}
          </button>
        </div>

        <div class="swagger-ui-wrapper" *ngIf="selectedService">
          <iframe 
            [src]="getSwaggerUrl(selectedService)" 
            class="swagger-iframe"
            title="Swagger UI">
          </iframe>
        </div>

        <div class="api-info">
          <h2>Available Services</h2>
          <div class="services-grid">
            <div *ngFor="let service of services" class="service-card">
              <h3>{{ service.name }}</h3>
              <p>{{ service.description }}</p>
              <a [href]="getSwaggerUrl(service.id)" target="_blank" class="docs-link">
                View Documentation →
              </a>
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
    }

    .service-tab:hover {
      background: #e8e8e8;
    }

    .service-tab.active {
      background: #2196F3;
      color: white;
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
    }

    .docs-link {
      display: inline-block;
      color: #2196F3;
      text-decoration: none;
      font-weight: 500;
      transition: color 0.3s ease;
    }

    .docs-link:hover {
      color: #1976D2;
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
    }
  `]
})
export class ApiDocsComponent implements OnInit {
  selectedService: string = 'user-service';

  services = [
    {
      id: 'user-service',
      name: 'User Service',
      description: 'User authentication, profile management, and AgriStack integration'
    },
    {
      id: 'weather-service',
      name: 'Weather Service',
      description: 'IMD weather forecasts, alerts, and agromet advisories'
    },
    {
      id: 'crop-service',
      name: 'Crop Service',
      description: 'Crop recommendations, rotation planning, and yield estimation'
    },
    {
      id: 'scheme-service',
      name: 'Scheme Service',
      description: 'Government schemes, eligibility assessment, and applications'
    },
    {
      id: 'mandi-service',
      name: 'Mandi Service',
      description: 'AGMARKNET price data, trends, and alerts'
    },
    {
      id: 'location-service',
      name: 'Location Service',
      description: 'GPS services, reverse geocoding, and government body locator'
    },
    {
      id: 'iot-service',
      name: 'IoT Service',
      description: 'IoT device management and sensor data collection'
    },
    {
      id: 'admin-service',
      name: 'Admin Service',
      description: 'Document management and system administration'
    }
  ];

  ngOnInit(): void {
    // Initialize with first service
    this.selectedService = this.services[0].id;
  }

  selectService(serviceId: string): void {
    this.selectedService = serviceId;
  }

  getSwaggerUrl(serviceId: string): string {
    const baseUrl = 'http://localhost:8080';
    const servicePort = this.getServicePort(serviceId);
    return `${baseUrl}:${servicePort}/swagger-ui.html`;
  }

  private getServicePort(serviceId: string): number {
    const portMap: { [key: string]: number } = {
      'user-service': 8099,
      'weather-service': 8100,
      'crop-service': 8093,
      'scheme-service': 8097,
      'mandi-service': 8096,
      'location-service': 8095,
      'iot-service': 8094,
      'admin-service': 8091
    };
    return portMap[serviceId] || 8080;
  }
}
