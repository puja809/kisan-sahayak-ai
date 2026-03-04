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
  templateUrl: './iot-dashboard.component.html',
  styleUrls: ['./iot-dashboard.component.css'],})
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
