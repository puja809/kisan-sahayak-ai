import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { VoiceAssistantService } from '../../services/voice-assistant.service';
import { LanguageService, Language } from '../../services/language.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

interface ConversationMessage {
  timestamp: Date;
  userText: string;
  systemResponse: string;
  userAudioPath?: string;
  systemAudioPath?: string;
  systemAudioBase64?: string;
  safeSystemResponse?: SafeHtml;
}

@Component({
  selector: 'app-voice-agent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './voice-agent.component.html',
  styleUrls: ['./voice-agent.component.css'],
})
export class VoiceAgentComponent implements OnInit, AfterViewChecked {
  @ViewChild('chatContainer') private chatContainer!: ElementRef;
  selectedLanguage = 'English';
  languages: Language[] = [];
  inputMode: 'text' | 'voice' = 'text';
  conversationHistory: ConversationMessage[] = [];
  isProcessing = false;
  textInput = '';
  isRecording = false;
  recordingDuration = 0;
  lastRecordingUrl: string | null = null;
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private recordingInterval: any = null;
  private userLatitude?: number;
  private userLongitude?: number;
  locationDenied = false;
  userCityName = '';

  constructor(
    private toastr: ToastrService,
    private voiceAssistantService: VoiceAssistantService,
    private languageService: LanguageService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
    this.loadConversationHistory();
    this.acquireUserLocation();
    this.languageService.getLanguages().subscribe({
      next: (response) => { this.languages = response.languages; },
      error: () => { /* fallback already handled inside LanguageService */ }
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    try {
      this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
    } catch (err) { }
  }

  private acquireUserLocation(): void {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.userLatitude = position.coords.latitude;
          this.userLongitude = position.coords.longitude;
          this.locationDenied = false;
          console.log(`User location acquired: ${this.userLatitude}, ${this.userLongitude}`);
        },
        (error) => {
          console.warn('Geolocation not available:', error.message);
          this.locationDenied = true;
        },
        { enableHighAccuracy: false, timeout: 10000 }
      );
    } else {
      this.locationDenied = true;
    }
  }

  onLanguageChange(): void {
    console.log('Language changed to:', this.selectedLanguage);
  }

  async switchMode(mode: 'text' | 'voice'): Promise<void> {
    this.inputMode = mode;
    this.textInput = '';
    if (mode === 'voice' && !this.isRecording) {
      await this.startRecording();
    }
  }

  async toggleRecording(): Promise<void> {
    if (this.isRecording) {
      this.stopRecording();
    } else {
      await this.startRecording();
    }
  }

  private async startRecording(): Promise<void> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaRecorder = new MediaRecorder(stream);
      this.audioChunks = [];

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/wav' });
        this.lastRecordingUrl = URL.createObjectURL(audioBlob);
        this.processVoiceInput(audioBlob);
        stream.getTracks().forEach(track => track.stop());
      };

      this.mediaRecorder.start();
      this.isRecording = true;
      this.recordingDuration = 0;

      this.recordingInterval = setInterval(() => {
        this.recordingDuration++;
        if (this.recordingDuration >= 30) {
          this.stopRecording();
        }
      }, 1000);
    } catch (error) {
      console.error('Error accessing microphone:', error);
      this.toastr.error('Could not access microphone. Please check permissions.');
    }
  }

  private stopRecording(): void {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.isRecording = false;
      clearInterval(this.recordingInterval);
    }
  }

  private processVoiceInput(audioBlob: Blob): void {
    this.isProcessing = true;

    // Create a temporary placeholder message
    const message: ConversationMessage = {
      timestamp: new Date(),
      userText: 'Processing voice...',
      systemResponse: ''
    };

    this.conversationHistory.push(message);
    const msgIndex = this.conversationHistory.length - 1;

    this.voiceAssistantService.askQuestionWithAudio(audioBlob, this.selectedLanguage, this.userLatitude, this.userLongitude, this.userCityName || undefined).subscribe({
      next: (response) => {
        this.lastRecordingUrl = null;

        if (response.success && (response.text || (response as any).answer)) {
          if (response.transcribed_text) {
            this.conversationHistory[msgIndex].userText = response.transcribed_text;
          }
          const rawResponse = (response.text || (response as any).answer);
          this.conversationHistory[msgIndex].systemResponse = rawResponse;
          this.conversationHistory[msgIndex].safeSystemResponse = this.sanitizer.bypassSecurityTrustHtml(this.formatSystemResponse(rawResponse));
          this.isProcessing = false;

          if (response.audio) {
            this.conversationHistory[msgIndex].systemAudioBase64 = response.audio;
            this.saveConversationHistory();
            this.toastr.success('Voice response received');

            console.log('Auto-playing AI audio response...');
            setTimeout(() => this.playAudio(response.audio!), 500);
          } else {
            this.saveConversationHistory();
          }
        } else {
          this.isProcessing = false;
          this.toastr.error('Failed to get an answer from the AI');
          this.conversationHistory[msgIndex].systemResponse = 'Error: ' + (response as any).error;
        }
      },
      error: (error) => {
        this.lastRecordingUrl = null;
        this.isProcessing = false;
        console.error('Voice processing error:', error);
        this.toastr.error('Failed to process voice input');
        this.conversationHistory[msgIndex].systemResponse = 'Connection error.';
      },
      complete: () => {
        this.isProcessing = false;
      }
    });
  }

  playAudio(base64Audio: string): void {
    try {
      console.log('Playing audio response, length:', base64Audio.length);

      // Decode base64 to binary string
      const binaryString = atob(base64Audio);
      const len = binaryString.length;
      const bytes = new Uint8Array(len);

      for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      // Create blob from bytes (MP3 from Polly)
      const audioBlob = new Blob([bytes], { type: 'audio/mpeg' });
      const audioUrl = URL.createObjectURL(audioBlob);

      console.log('Audio blob created, size:', audioBlob.size, 'bytes');

      const audio = new Audio();
      audio.src = audioUrl;
      audio.volume = 1.0;

      audio.oncanplay = () => {
        console.log('Audio ready to play');
      };

      audio.onended = () => {
        console.log('Audio playback completed');
        URL.revokeObjectURL(audioUrl);
      };

      audio.onerror = (e) => {
        console.error('Audio playback error:', e);
        URL.revokeObjectURL(audioUrl);
      };

      const playPromise = audio.play();
      if (playPromise !== undefined) {
        playPromise
          .then(() => console.log('Audio playing'))
          .catch(err => {
            console.error('Error playing audio:', err);
            URL.revokeObjectURL(audioUrl);
          });
      }
    } catch (error) {
      console.error('Error playing audio:', error);
    }
  }

  sendTextQuery(): void {
    if (!this.textInput.trim() || this.isProcessing) return;

    this.isProcessing = true;
    const userText = this.textInput;
    this.textInput = '';

    console.log('Sending query to Voice Assistant stream:', userText);

    const message: ConversationMessage = {
      timestamp: new Date(),
      userText,
      systemResponse: ''
    };

    this.conversationHistory.push(message);
    const msgIndex = this.conversationHistory.length - 1;

    this.voiceAssistantService.askQuestion(userText, this.selectedLanguage, this.userLatitude, this.userLongitude, this.userCityName || undefined).subscribe({
      next: (response) => {
        this.isProcessing = false;
        if (response.success && response.answer) {
          const rawResponse = response.answer;
          this.conversationHistory[msgIndex].systemResponse = rawResponse;
          this.conversationHistory[msgIndex].safeSystemResponse = this.sanitizer.bypassSecurityTrustHtml(this.formatSystemResponse(rawResponse));
          this.saveConversationHistory();
          this.toastr.success('Answer generated successfully');
        } else {
          this.conversationHistory[msgIndex].systemResponse = 'Failed to generate an answer.';
          this.toastr.error('The server did not return an answer.');
        }
      },
      error: (error) => {
        this.isProcessing = false;
        console.error('Error querying Voice Assistant:', error);
        this.toastr.error('Failed to connect to Voice Assistant service', 'Connection Error');

        this.conversationHistory[msgIndex].systemResponse = 'Sorry, I am unable to connect to the Voice Assistant service right now. Please try again later.';
        this.saveConversationHistory();
      },
      complete: () => {
        this.isProcessing = false;
        this.saveConversationHistory();
      }
    });
  }

  clearHistory(): void {
    this.conversationHistory = [];
    this.saveConversationHistory();
    this.toastr.info('Chat history cleared');
  }

  private saveConversationHistory(): void {
    if (this.conversationHistory.length === 0) {
      localStorage.removeItem('krishi_rag_history');
    } else {
      localStorage.setItem('krishi_rag_history', JSON.stringify(this.conversationHistory));
    }
  }

  private loadConversationHistory(): void {
    const saved = localStorage.getItem('krishi_rag_history');
    if (saved) {
      try {
        const history: ConversationMessage[] = JSON.parse(saved);
        this.conversationHistory = history.map(msg => ({
          ...msg,
          safeSystemResponse: msg.systemResponse ? this.sanitizer.bypassSecurityTrustHtml(this.formatSystemResponse(msg.systemResponse)) : undefined
        }));
      } catch (e) {
        console.error('Failed to parse history', e);
      }
    }
  }

  private formatSystemResponse(text: string): string {
    if (!text) return '';

    // 1. Convert Markdown bold (**text**) to HTML (<b>text</b>)
    let formatted = text.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>');

    // 2. Bold specific agricultural labels if not already bolded
    const labelsToBold = [
      "Previous Crop:",
      "Current Season:",
      "Recommended Crop:"
    ];

    labelsToBold.forEach(label => {
      // Find label that is NOT already inside <b> or <strong>
      const regex = new RegExp(`(?<!<b>|<strong>)${label}`, 'g');
      formatted = formatted.replace(regex, `<b>${label}</b>`);
    });

    // 3. Add newline before numbered items like "1. ", "2. ", etc.
    formatted = formatted.replace(/(\s)(\d+\.)/g, '\n$2');

    // 4. Add newline before common concluding advice/sentences
    const helpSentences = [
      "This rotation helps",
      "Ensure to follow",
      "For more details",
      "Should you have any"
    ];

    helpSentences.forEach(sentence => {
      const regex = new RegExp(`(\\s)(${sentence})`, 'g');
      formatted = formatted.replace(regex, '\n$2');
    });

    return formatted.trim();
  }
}
