import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DiseaseDetectionComponent } from './disease-detection.component';
import { ToastrService } from 'ngx-toastr';

describe('DiseaseDetectionComponent', () => {
  let component: DiseaseDetectionComponent;
  let fixture: ComponentFixture<DiseaseDetectionComponent>;
  let toastr: ToastrService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DiseaseDetectionComponent, HttpClientTestingModule],
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

    fixture = TestBed.createComponent(DiseaseDetectionComponent);
    component = fixture.componentInstance;
    toastr = TestBed.inject(ToastrService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with no selected file', () => {
    expect(component.selectedFile).toBeNull();
  });

  it('should initialize with no detection result', () => {
    expect(component.detectionResult).toBeNull();
  });

  it('should initialize with empty detection history', () => {
    expect(component.detectionHistory).toEqual([]);
  });

  it('should handle file selection', () => {
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const event = {
      target: {
        files: [file]
      }
    } as any;

    component.onFileSelected(event);

    expect(component.selectedFile).toBe(file);
  });

  it('should reject files larger than 10MB', () => {
    const largeFile = new File(['x'.repeat(11 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const event = {
      target: {
        files: [largeFile]
      }
    } as any;

    component.onFileSelected(event);

    expect(component.selectedFile).toBeNull();
    expect(toastr.error).toHaveBeenCalledWith('File size must be less than 10MB');
  });

  it('should handle drag over', () => {
    const event = new DragEvent('dragover');
    spyOn(event, 'preventDefault');

    component.onDragOver(event);

    expect(component.isDragOver).toBe(true);
    expect(event.preventDefault).toHaveBeenCalled();
  });

  it('should handle drag leave', () => {
    component.isDragOver = true;
    const event = new DragEvent('dragleave');
    spyOn(event, 'preventDefault');

    component.onDragLeave(event);

    expect(component.isDragOver).toBe(false);
    expect(event.preventDefault).toHaveBeenCalled();
  });

  it('should handle drop', () => {
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const event = new DragEvent('drop', {
      dataTransfer: new DataTransfer()
    });
    event.dataTransfer?.items.add(file);
    spyOn(event, 'preventDefault');

    component.onDrop(event);

    expect(component.isDragOver).toBe(false);
    expect(event.preventDefault).toHaveBeenCalled();
  });

  it('should determine confidence level as high', () => {
    const level = component.getConfidenceLevel(0.85);
    expect(level).toBe('high');
  });

  it('should determine confidence level as medium', () => {
    const level = component.getConfidenceLevel(0.65);
    expect(level).toBe('medium');
  });

  it('should determine confidence level as low', () => {
    const level = component.getConfidenceLevel(0.5);
    expect(level).toBe('low');
  });

  it('should call findNearbyKVK method', () => {
    spyOn(console, 'log');
    component.findNearbyKVK();
    expect(console.log).toHaveBeenCalledWith('Finding nearby KVK');
  });
});
