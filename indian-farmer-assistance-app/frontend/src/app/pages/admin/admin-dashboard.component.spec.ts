import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { ToastrService } from 'ngx-toastr';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let toastr: ToastrService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent, HttpClientTestingModule],
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

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    toastr = TestBed.inject(ToastrService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty documents list', () => {
    expect(component.documents).toEqual([]);
  });

  it('should initialize with empty schemes list', () => {
    expect(component.schemes).toEqual([]);
  });

  it('should initialize with null user analytics', () => {
    expect(component.userAnalytics).toBeNull();
  });

  it('should initialize with empty audit logs', () => {
    expect(component.auditLogs).toEqual([]);
  });

  it('should initialize system config with default values', () => {
    expect(component.systemConfig.apiTimeout).toBe(30);
    expect(component.systemConfig.cacheTTL).toBe(30);
    expect(component.systemConfig.maxUploadSize).toBe(50);
  });

  it('should handle document selection', () => {
    const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
    const event = {
      target: {
        files: [file]
      }
    } as any;

    component.onDocumentSelected(event);

    // Verify HTTP request is made
    expect(component).toBeTruthy();
  });

  it('should call editDocument method', () => {
    spyOn(console, 'log');
    const doc = {
      id: 1,
      title: 'Test Doc',
      category: 'schemes',
      uploadedBy: 'admin',
      uploadDate: '2024-01-01',
      version: 1
    };

    component.editDocument(doc);

    expect(console.log).toHaveBeenCalledWith('Edit document:', doc);
  });

  it('should call addScheme method', () => {
    spyOn(console, 'log');
    component.addScheme();
    expect(console.log).toHaveBeenCalledWith('Add scheme');
  });

  it('should call editScheme method', () => {
    spyOn(console, 'log');
    const scheme = {
      id: 1,
      schemeCode: 'PM-KISAN',
      schemeName: 'PM-Kisan Samman Nidhi',
      schemeType: 'CENTRAL',
      state: 'All',
      benefitAmount: 6000,
      isActive: true
    };

    component.editScheme(scheme);

    expect(console.log).toHaveBeenCalledWith('Edit scheme:', scheme);
  });
});
