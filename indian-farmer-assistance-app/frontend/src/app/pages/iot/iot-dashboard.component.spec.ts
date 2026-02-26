import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { IoTDashboardComponent } from './iot-dashboard.component';
import { ToastrService } from 'ngx-toastr';

describe('IoTDashboardComponent', () => {
  let component: IoTDashboardComponent;
  let fixture: ComponentFixture<IoTDashboardComponent>;
  let toastr: ToastrService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IoTDashboardComponent, HttpClientTestingModule],
      providers: [
        {
          provide: ToastrService,
          useValue: {
            success: jasmine.createSpy('success'),
            error: jasmine.createSpy('error')
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IoTDashboardComponent);
    component = fixture.componentInstance;
    toastr = TestBed.inject(ToastrService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty devices list', () => {
    expect(component.devices).toEqual([]);
  });

  it('should initialize with no selected device', () => {
    expect(component.selectedDevice).toBeNull();
  });

  it('should initialize with no current reading', () => {
    expect(component.currentReading).toBeNull();
  });

  it('should initialize with empty historical data', () => {
    expect(component.historicalData).toEqual([]);
  });

  it('should initialize alert config with default values', () => {
    expect(component.alertConfig.soilMoistureMin).toBe(30);
    expect(component.alertConfig.soilMoistureMax).toBe(70);
    expect(component.alertConfig.temperatureMin).toBe(15);
    expect(component.alertConfig.temperatureMax).toBe(35);
    expect(component.alertConfig.humidityMin).toBe(40);
    expect(component.alertConfig.humidityMax).toBe(80);
  });

  it('should initialize with empty new device fields', () => {
    expect(component.newDeviceName).toBe('');
    expect(component.newDeviceType).toBe('');
  });

  it('should calculate average soil moisture', () => {
    component.historicalData = [
      {
        timestamp: '2024-01-01',
        soilMoisture: 50,
        temperature: 25,
        humidity: 60,
        phLevel: 7,
        ecValue: 1.5,
        npkNitrogen: 100,
        npkPhosphorus: 50,
        npkPotassium: 150
      },
      {
        timestamp: '2024-01-02',
        soilMoisture: 60,
        temperature: 26,
        humidity: 65,
        phLevel: 7.1,
        ecValue: 1.6,
        npkNitrogen: 110,
        npkPhosphorus: 55,
        npkPotassium: 160
      }
    ];

    const average = component.getAverage('soilMoisture');

    expect(average).toBe(55);
  });

  it('should calculate average temperature', () => {
    component.historicalData = [
      {
        timestamp: '2024-01-01',
        soilMoisture: 50,
        temperature: 24,
        humidity: 60,
        phLevel: 7,
        ecValue: 1.5,
        npkNitrogen: 100,
        npkPhosphorus: 50,
        npkPotassium: 150
      },
      {
        timestamp: '2024-01-02',
        soilMoisture: 60,
        temperature: 26,
        humidity: 65,
        phLevel: 7.1,
        ecValue: 1.6,
        npkNitrogen: 110,
        npkPhosphorus: 55,
        npkPotassium: 160
      }
    ];

    const average = component.getAverage('temperature');

    expect(average).toBe(25);
  });

  it('should return 0 average when no historical data', () => {
    component.historicalData = [];

    const average = component.getAverage('soilMoisture');

    expect(average).toBe(0);
  });

  it('should call addDevice method', () => {
    spyOn(console, 'log');
    component.addDevice();
    // Method is empty, just verify it doesn't throw
    expect(component).toBeTruthy();
  });
});
