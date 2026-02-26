import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../services/auth.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, HttpClientTestingModule],
      providers: [AuthService]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display current user information', () => {
    const mockUser = {
      farmerId: 'FARMER123',
      name: 'John Farmer',
      phone: '9876543210',
      preferredLanguage: 'en',
      state: 'Karnataka',
      district: 'Bangalore'
    };

    spyOn(authService, 'getCurrentUser').and.returnValue(mockUser);

    component.ngOnInit();

    expect(component.currentUser).toEqual(mockUser);
  });

  it('should initialize with empty crops list', () => {
    expect(component.crops).toEqual([]);
  });

  it('should initialize with empty activities list', () => {
    expect(component.activities).toEqual([]);
  });

  it('should initialize with empty yield predictions list', () => {
    expect(component.yieldPredictions).toEqual([]);
  });

  it('should have null financial summary initially', () => {
    expect(component.financialSummary).toBeNull();
  });

  it('should call addCrop method', () => {
    spyOn(console, 'log');
    component.addCrop();
    expect(console.log).toHaveBeenCalledWith('Add crop');
  });

  it('should call recordHarvest method', () => {
    spyOn(console, 'log');
    component.recordHarvest();
    expect(console.log).toHaveBeenCalledWith('Record harvest');
  });

  it('should call checkWeather method', () => {
    spyOn(console, 'log');
    component.checkWeather();
    expect(console.log).toHaveBeenCalledWith('Check weather');
  });

  it('should call viewSchemes method', () => {
    spyOn(console, 'log');
    component.viewSchemes();
    expect(console.log).toHaveBeenCalledWith('View schemes');
  });
});
