import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { VoiceAgentComponent } from './voice-agent.component';
import { ToastrService } from 'ngx-toastr';

describe('VoiceAgentComponent', () => {
  let component: VoiceAgentComponent;
  let fixture: ComponentFixture<VoiceAgentComponent>;
  let toastr: ToastrService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VoiceAgentComponent, HttpClientTestingModule],
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

    fixture = TestBed.createComponent(VoiceAgentComponent);
    component = fixture.componentInstance;
    toastr = TestBed.inject(ToastrService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with English language', () => {
    expect(component.selectedLanguage).toBe('en');
  });

  it('should initialize with empty conversation history', () => {
    expect(component.conversationHistory).toEqual([]);
  });

  it('should initialize with recording disabled', () => {
    expect(component.isRecording).toBe(false);
  });

  it('should initialize with processing disabled', () => {
    expect(component.isProcessing).toBe(false);
  });

  it('should initialize with empty text input', () => {
    expect(component.textInput).toBe('');
  });

  it('should initialize with no audio path', () => {
    expect(component.lastAudioPath).toBeNull();
  });

  it('should toggle recording state', () => {
    expect(component.isRecording).toBe(false);

    component.toggleRecording();

    expect(component.isRecording).toBe(true);

    component.toggleRecording();

    expect(component.isRecording).toBe(false);
  });

  it('should handle language change', () => {
    spyOn(console, 'log');
    component.selectedLanguage = 'hi';

    component.onLanguageChange();

    expect(console.log).toHaveBeenCalledWith('Language changed to:', 'hi');
  });

  it('should clear conversation history', () => {
    component.conversationHistory = [
      {
        timestamp: new Date(),
        userText: 'Hello',
        systemResponse: 'Hi there'
      }
    ];
    component.lastAudioPath = 'path/to/audio.mp3';

    component.clearHistory();

    expect(component.conversationHistory).toEqual([]);
    expect(component.lastAudioPath).toBeNull();
  });

  it('should not send empty text query', () => {
    component.textInput = '';
    spyOn(console, 'log');

    component.sendTextQuery();

    expect(console.log).not.toHaveBeenCalled();
  });

  it('should not send whitespace-only text query', () => {
    component.textInput = '   ';
    spyOn(console, 'log');

    component.sendTextQuery();

    expect(console.log).not.toHaveBeenCalled();
  });

  it('should support multiple languages', () => {
    const languages = ['en', 'hi', 'ta', 'te', 'ka', 'ml', 'mr', 'gu', 'pa', 'bn'];

    languages.forEach(lang => {
      component.selectedLanguage = lang;
      expect(component.selectedLanguage).toBe(lang);
    });
  });
});
