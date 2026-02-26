import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

interface IoTDevice {
  id: number;
  deviceId: string;
  deviceName: string;
  deviceType: string;
  status: string;
  lastSeen: string;
  configuration: any;
}

interface SensorReading {
  timestamp: string;
  soilMoisture: number;
  temperature: number;
  humidity: number;
  phLevel: number;
  ecValue: number;
  npkNitrogen: number;
  npkPhosphorus: number;
  npkPotassium: number;
}

interface AlertConfig {
  deviceId: number;
  soilMoistureMin: number;
  soilMoistureMax: number;
  temperatureMin: number;
  temperatureMax: number;
  humidityMin: number;
  humidityMax: number;
}

@Component({
  selector: 'app-iot-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="iot-dashboard-container">
      <div class="header">
        <h1>IoT Device Management</h1>
        <button class="btn-primary" (click)="addDevice()">+ Add Device</button>
      </div>

      <div class="iot-grid">
        <!-- Device List -->
        <div class="card devices-card">
          <h2>Connected Devices</h2>
          <div *ngIf="devices.length > 0" class="devices-list">
            <div *ngFor="let device of devices" class="device-item" [class.offline]="device.status === 'OFFLINE'">
              <div class="device-header">
                <h3>{{ device.deviceName }}</h3>
                <span class="status-badge" [class]="'status-' + device.status.toLowerCase()">
                  {{ device.status }}
                </span>
              </div>
              <p class="device-id">ID: {{ device.deviceId }}</p>
              <p class="device-type">Type: {{ device.deviceType }}</p>
              <p class="last-seen">Last seen: {{ device.lastSeen | date: 'short' }}</p>
              <div class="device-actions">
                <button class="btn-small" (click)="viewDevice(device)">View</button>
                <button class="btn-small" (click)="configureDevice(device)">Configure</button>
                <button class="btn-small danger" (click)="removeDevice(device)">Remove</button>
              </div>
            </div>
          </div>
          <p *ngIf="devices.length === 0" class="no-data">No devices connected</p>
        </div>

        <!-- Real-time Sensor Dashboard -->
        <div class="card sensor-card" *ngIf="selectedDevice && currentReading">
          <h2>{{ selectedDevice.deviceName }} - Real-time Readings</h2>
          <div class="sensor-grid">
            <div class="sensor-item">
              <span class="label">Soil Moisture</span>
              <span class="value">{{ currentReading.soilMoisture }}%</span>
              <div class="progress-bar">
                <div class="progress" [style.width.%]="currentReading.soilMoisture"></div>
              </div>
            </div>

            <div class="sensor-item">
              <span class="label">Temperature</span>
              <span class="value">{{ currentReading.temperature }}°C</span>
            </div>

            <div class="sensor-item">
              <span class="label">Humidity</span>
              <span class="value">{{ currentReading.humidity }}%</span>
              <div class="progress-bar">
                <div class="progress" [style.width.%]="currentReading.humidity"></div>
              </div>
            </div>

            <div class="sensor-item">
              <span class="label">pH Level</span>
              <span class="value">{{ currentReading.phLevel }}</span>
            </div>

            <div class="sensor-item">
              <span class="label">EC Value</span>
              <span class="value">{{ currentReading.ecValue }}</span>
            </div>

            <div class="sensor-item">
              <span class="label">Nitrogen (N)</span>
              <span class="value">{{ currentReading.npkNitrogen }} mg/kg</span>
            </div>

            <div class="sensor-item">
              <span class="label">Phosphorus (P)</span>
              <span class="value">{{ currentReading.npkPhosphorus }} mg/kg</span>
            </div>

            <div class="sensor-item">
              <span class="label">Potassium (K)</span>
              <span class="value">{{ currentReading.npkPotassium }} mg/kg</span>
            </div>
          </div>
          <p class="reading-time">Last updated: {{ currentReading.timestamp | date: 'short' }}</p>
        </div>

        <!-- Alert Configuration -->
        <div class="card alert-card" *ngIf="selectedDevice">
          <h2>Alert Configuration</h2>
          <div class="alert-config">
            <div class="config-group">
              <label>Soil Moisture Range (%)</label>
              <div class="range-inputs">
                <input type="number" [(ngModel)]="alertConfig.soilMoistureMin" placeholder="Min" />
                <span>-</span>
                <input type="number" [(ngModel)]="alertConfig.soilMoistureMax" placeholder="Max" />
              </div>
            </div>

            <div class="config-group">
              <label>Temperature Range (°C)</label>
              <div class="range-inputs">
                <input type="number" [(ngModel)]="alertConfig.temperatureMin" placeholder="Min" />
                <span>-</span>
                <input type="number" [(ngModel)]="alertConfig.temperatureMax" placeholder="Max" />
              </div>
            </div>

            <div class="config-group">
              <label>Humidity Range (%)</label>
              <div class="range-inputs">
                <input type="number" [(ngModel)]="alertConfig.humidityMin" placeholder="Min" />
                <span>-</span>
                <input type="number" [(ngModel)]="alertConfig.humidityMax" placeholder="Max" />
              </div>
            </div>

            <button class="btn-primary" (click)="saveAlertConfig()">Save Configuration</button>
          </div>
        </div>

        <!-- Historical Trends -->
        <div class="card trends-card" *ngIf="selectedDevice && historicalData.length > 0">
          <h2>30-Day Trends</h2>
          <div class="trends-info">
            <p>Soil Moisture: {{ getAverage('soilMoisture') }}% avg</p>
            <p>Temperature: {{ getAverage('temperature') }}°C avg</p>
            <p>Humidity: {{ getAverage('humidity') }}% avg</p>
          </div>
          <p class="chart-placeholder">Chart visualization would be displayed here</p>
        </div>

        <!-- Device Provisioning -->
        <div class="card provisioning-card">
          <h2>Add New Device</h2>
          <div class="provisioning-form">
            <input type="text" [(ngModel)]="newDeviceName" placeholder="Device Name" />
            <select [(ngModel)]="newDeviceType">
              <option value="">Select Device Type</option>
              <option value="SOIL_SENSOR">Soil Sensor</option>
              <option value="WEATHER_STATION">Weather Station</option>
              <option value="MOISTURE_SENSOR">Moisture Sensor</option>
            </select>
            <button class="btn-primary" (click)="provisionDevice()" [disabled]="!newDeviceName || !newDeviceType">
              Provision Device
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .iot-dashboard-container {
      padding: 1.5rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    .header h1 {
      font-size: 2rem;
      color: #333;
      margin: 0;
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

    .btn-primary:hover:not(:disabled) {
      background: #5568d3;
    }

    .btn-primary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .iot-grid {
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

    .devices-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .device-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      border-left: 4px solid #27ae60;
    }

    .device-item.offline {
      border-left-color: #e74c3c;
      opacity: 0.7;
    }

    .device-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .device-header h3 {
      margin: 0;
      color: #333;
    }

    .status-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 20px;
      font-size: 0.75rem;
      font-weight: 600;
      color: white;
    }

    .status-active {
      background: #27ae60;
    }

    .status-inactive {
      background: #95a5a6;
    }

    .status-offline {
      background: #e74c3c;
    }

    .device-id,
    .device-type,
    .last-seen {
      margin: 0.25rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .device-actions {
      display: flex;
      gap: 0.5rem;
      margin-top: 1rem;
    }

    .btn-small {
      flex: 1;
      padding: 0.5rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .btn-small:hover {
      background: #5568d3;
    }

    .btn-small.danger {
      background: #e74c3c;
    }

    .btn-small.danger:hover {
      background: #c0392b;
    }

    .no-data {
      color: #999;
      font-style: italic;
    }

    .sensor-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .sensor-item {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 4px;
      text-align: center;
    }

    .sensor-item .label {
      display: block;
      color: #666;
      font-size: 0.85rem;
      margin-bottom: 0.5rem;
    }

    .sensor-item .value {
      display: block;
      font-size: 1.5rem;
      font-weight: 600;
      color: #333;
      margin-bottom: 0.5rem;
    }

    .progress-bar {
      width: 100%;
      height: 4px;
      background: #ddd;
      border-radius: 2px;
      overflow: hidden;
    }

    .progress {
      height: 100%;
      background: #667eea;
      transition: width 0.3s;
    }

    .reading-time {
      color: #999;
      font-size: 0.85rem;
      margin: 1rem 0 0 0;
    }

    .alert-config {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .config-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .config-group label {
      color: #333;
      font-weight: 600;
    }

    .range-inputs {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    .range-inputs input {
      flex: 1;
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .range-inputs span {
      color: #666;
    }

    .trends-info {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
      margin-bottom: 1rem;
    }

    .trends-info p {
      margin: 0.5rem 0;
      color: #666;
    }

    .chart-placeholder {
      text-align: center;
      color: #999;
      padding: 2rem;
      background: #f8f9fa;
      border-radius: 4px;
    }

    .provisioning-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .provisioning-form input,
    .provisioning-form select {
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .provisioning-form input:focus,
    .provisioning-form select:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    @media (max-width: 768px) {
      .iot-grid {
        grid-template-columns: 1fr;
      }

      .sensor-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `]
})
export class IoTDashboardComponent implements OnInit {
  devices: IoTDevice[] = [];
  selectedDevice: IoTDevice | null = null;
  currentReading: SensorReading | null = null;
  historicalData: SensorReading[] = [];
  alertConfig: AlertConfig = {
    deviceId: 0,
    soilMoistureMin: 30,
    soilMoistureMax: 70,
    temperatureMin: 15,
    temperatureMax: 35,
    humidityMin: 40,
    humidityMax: 80
  };
  newDeviceName = '';
  newDeviceType = '';

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadDevices();
  }

  private loadDevices(): void {
    // Ideally we fetch from AuthService, but hardcoding 'FARMER123' as fallback or test
    const farmerId = 'FARMER123';
    this.http.get<IoTDevice[]>(`/api/v1/iot/devices/${farmerId}`).subscribe({
      next: (devices) => {
        this.devices = devices;
        if (devices.length > 0) {
          this.viewDevice(devices[0]);
        }
      },
      error: (error) => console.error('Failed to load devices:', error)
    });
  }

  viewDevice(device: IoTDevice): void {
    this.selectedDevice = device;
    this.alertConfig.deviceId = device.id;
    this.loadCurrentReading();
    this.loadHistoricalData();
  }

  private loadCurrentReading(): void {
    if (!this.selectedDevice) return;
    this.http.get<SensorReading>(`/api/v1/iot/devices/${this.selectedDevice.id}/readings`).subscribe({
      next: (reading) => this.currentReading = reading,
      error: (error) => console.error('Failed to load current reading:', error)
    });
  }

  private loadHistoricalData(): void {
    if (!this.selectedDevice) return;
    this.http.get<SensorReading[]>(`/api/v1/iot/devices/${this.selectedDevice.id}/readings/history`).subscribe({
      next: (data) => this.historicalData = data,
      error: (error) => console.error('Failed to load historical data:', error)
    });
  }

  configureDevice(device: IoTDevice): void {
    this.viewDevice(device);
  }

  removeDevice(device: IoTDevice): void {
    if (confirm('Are you sure you want to remove this device?')) {
      this.http.delete(`/api/v1/iot/devices/${device.id}`).subscribe({
        next: () => {
          this.toastr.success('Device removed');
          this.loadDevices();
        },
        error: (error) => this.toastr.error('Failed to remove device')
      });
    }
  }

  addDevice(): void {
    // Navigate to device provisioning
  }

  provisionDevice(): void {
    if (!this.newDeviceName || !this.newDeviceType) return;

    this.http.post('/api/v1/iot/devices/provision', {
      deviceName: this.newDeviceName,
      deviceType: this.newDeviceType
    }).subscribe({
      next: () => {
        this.toastr.success('Device provisioned successfully');
        this.newDeviceName = '';
        this.newDeviceType = '';
        this.loadDevices();
      },
      error: (error) => this.toastr.error('Failed to provision device')
    });
  }

  saveAlertConfig(): void {
    this.http.post(`/api/v1/iot/devices/${this.alertConfig.deviceId}/alerts/config`, this.alertConfig).subscribe({
      next: () => this.toastr.success('Alert configuration saved'),
      error: (error) => this.toastr.error('Failed to save configuration')
    });
  }

  getAverage(field: keyof SensorReading): number {
    if (this.historicalData.length === 0) return 0;
    const sum = this.historicalData.reduce((acc, reading) => acc + (reading[field] as number), 0);
    return Math.round(sum / this.historicalData.length);
  }
}
