import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { VoiceAssistantService } from '../../services/voice-assistant.service';
import { LanguageService, Language } from '../../services/language.service';

interface ConversationMessage {
  timestamp: Date;
  userText: string;
  systemResponse: string;
  userAudioPath?: string;
  systemAudioPath?: string;
  systemAudioBase64?: string;
}

@Component({
  selector: 'app-voice-agent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="voice-agent-container">
      <div class="voice-header">
        <div class="header-content">
          <h1>🌾 Krishi Assistant</h1>
          <p>Ask questions about farming, schemes, and agriculture</p>
        </div>
        <div class="language-selector">
          <label for="language">Language:</label>
          <select id="language" [(ngModel)]="selectedLanguage" (change)="onLanguageChange()">
            <option *ngFor="let lang of languages" [value]="lang.code">
              {{ lang.nativeName }} ({{ lang.name }})
            </option>
          </select>
        </div>
      </div>

      <!-- Mode Toggle -->
      <div class="mode-toggle">
        <button 
          class="toggle-btn" 
          [class.active]="inputMode === 'text'"
          (click)="switchMode('text')"
        >
          💬 Text Chat
        </button>
        <button 
          class="toggle-btn" 
          [class.active]="inputMode === 'voice'"
          (click)="switchMode('voice')"
        >
          🎤 Voice
        </button>
      </div>

      <div class="voice-content">
        <!-- Conversation History -->
        <div class="conversation-history">
          <div class="history-header">
            <h2>Chat History</h2>
            <button 
              class="btn-clear" 
              (click)="clearHistory()"
              [disabled]="conversationHistory.length === 0"
              title="Clear all messages"
            >
              ✕
            </button>
          </div>
          <div class="messages-container">
            <div *ngIf="conversationHistory.length === 0" class="welcome-message">
              <p>👋 Welcome! Ask me anything about agriculture, farming schemes, or government support.</p>
            </div>
            <div *ngFor="let message of conversationHistory" class="message-group">
              <div class="user-message">
                <p>{{ message.userText }}</p>
              </div>
              <div class="system-message">
                <p>{{ message.systemResponse }}</p>
                <button 
                  *ngIf="message.systemAudioBase64" 
                  class="btn-play-audio"
                  (click)="playAudio(message.systemAudioBase64!)"
                  title="Play audio response"
                >
                  🔊 Play Audio
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Text Input Section -->
        <div class="input-section" *ngIf="inputMode === 'text'">
          <div class="text-input-group">
            <input
              type="text"
              [(ngModel)]="textInput"
              placeholder="Type your question here..."
              (keyup.enter)="sendTextQuery()"
              class="text-input"
              [disabled]="isProcessing"
              autocomplete="off"
            />
            <button 
              class="btn-send" 
              (click)="sendTextQuery()" 
              [disabled]="!textInput.trim() || isProcessing"
            >
              {{ isProcessing ? '⏳' : '➤' }}
            </button>
          </div>
          <div *ngIf="isProcessing" class="processing-indicator">
            <div class="spinner"></div>
            <p>Getting answer...</p>
          </div>
        </div>

        <!-- Voice Input Section -->
        <div class="input-section" *ngIf="inputMode === 'voice'">
          <div class="voice-controls">
            <button 
              class="btn-record" 
              [class.recording]="isRecording"
              (click)="toggleRecording()"
              [disabled]="isProcessing"
            >
              {{ isRecording ? '⏹ Stop' : '🎤 Record' }}
            </button>
            <div class="recording-status" *ngIf="isRecording">
              <span class="pulse"></span>
              Recording... {{ recordingDuration }}s
            </div>
          </div>
          <div *ngIf="isProcessing" class="processing-indicator">
            <div class="spinner"></div>
            <p>Processing voice input...</p>
          </div>
          <div *ngIf="lastRecordingUrl" class="audio-preview">
            <p>Recorded audio:</p>
            <audio controls [src]="lastRecordingUrl"></audio>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .voice-agent-container {
      padding: 1.5rem;
      max-width: 900px;
      margin: 0 auto;
      height: 100vh;
      display: flex;
      flex-direction: column;
      background: linear-gradient(135deg, #2d5016 0%, #3d6b1f 100%);
    }

    .voice-header {
      background: white;
      padding: 1.5rem;
      border-radius: 12px;
      margin-bottom: 1.5rem;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 2rem;
    }

    .header-content h1 {
      font-size: 1.75rem;
      color: #2d5016;
      margin: 0 0 0.25rem 0;
    }

    .header-content p {
      color: #666;
      margin: 0;
      font-size: 0.9rem;
    }

    .language-selector {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      white-space: nowrap;
    }

    .language-selector label {
      font-weight: 600;
      color: #333;
      font-size: 0.9rem;
    }

    .language-selector select {
      padding: 0.5rem 0.75rem;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 0.9rem;
      background: white;
      cursor: pointer;
    }

    .mode-toggle {
      display: flex;
      gap: 1rem;
      margin-bottom: 1.5rem;
      background: white;
      padding: 0.75rem;
      border-radius: 12px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .toggle-btn {
      flex: 1;
      padding: 0.75rem 1.5rem;
      border: 2px solid #ddd;
      background: white;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      font-size: 1rem;
      transition: all 0.3s;
      color: #666;
    }

    .toggle-btn:hover:not(:disabled) {
      border-color: #3d6b1f;
      color: #3d6b1f;
    }

    .toggle-btn.active {
      background: #3d6b1f;
      color: white;
      border-color: #3d6b1f;
    }

    .toggle-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .voice-content {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      flex: 1;
      overflow: hidden;
    }

    .conversation-history {
      background: white;
      padding: 1.5rem;
      border-radius: 12px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      display: flex;
      flex-direction: column;
      flex: 1;
      overflow: hidden;
    }

    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      border-bottom: 2px solid #3d6b1f;
      padding-bottom: 0.75rem;
    }

    .history-header h2 {
      font-size: 1.25rem;
      color: #333;
      margin: 0;
    }

    .btn-clear {
      background: #e74c3c;
      color: white;
      border: none;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      cursor: pointer;
      font-size: 1.2rem;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;
    }

    .btn-clear:hover:not(:disabled) {
      background: #c0392b;
      transform: scale(1.1);
    }

    .btn-clear:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding-right: 0.5rem;
    }

    .messages-container::-webkit-scrollbar {
      width: 6px;
    }

    .messages-container::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }

    .messages-container::-webkit-scrollbar-thumb {
      background: #3d6b1f;
      border-radius: 3px;
    }

    .welcome-message {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      text-align: center;
    }

    .welcome-message p {
      color: #999;
      font-size: 1.1rem;
      line-height: 1.6;
      max-width: 400px;
    }

    .message-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .user-message {
      background: #d4edda;
      padding: 1rem;
      border-radius: 8px;
      margin-left: auto;
      max-width: 80%;
      border-bottom-right-radius: 2px;
    }

    .user-message p {
      margin: 0;
      color: #155724;
    }

    .system-message {
      background: #e8f5e9;
      padding: 1rem;
      border-radius: 8px;
      margin-right: auto;
      max-width: 80%;
      border-left: 4px solid #3d6b1f;
      border-bottom-left-radius: 2px;
    }

    .system-message p {
      margin: 0;
      color: #1b5e20;
      white-space: pre-wrap;
      line-height: 1.5;
    }

    .btn-play-audio {
      margin-top: 0.75rem;
      padding: 0.5rem 1rem;
      background: #3d6b1f;
      color: white;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.85rem;
      transition: all 0.2s;
    }

    .btn-play-audio:hover {
      background: #2d5016;
    }

    .input-section {
      background: white;
      padding: 1.5rem;
      border-radius: 12px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .text-input-group {
      display: flex;
      gap: 0.75rem;
    }

    .text-input {
      flex: 1;
      padding: 0.75rem 1rem;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 1rem;
      transition: all 0.3s;
    }

    .text-input:focus {
      outline: none;
      border-color: #3d6b1f;
      box-shadow: 0 0 0 3px rgba(61, 107, 31, 0.1);
    }

    .text-input:disabled {
      background-color: #f5f5f5;
      cursor: not-allowed;
    }

    .btn-send {
      padding: 0.75rem 1.5rem;
      background: #3d6b1f;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      font-size: 1.2rem;
      transition: all 0.3s;
      min-width: 50px;
    }

    .btn-send:hover:not(:disabled) {
      background: #2d5016;
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(61, 107, 31, 0.3);
    }

    .btn-send:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .processing-indicator {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #e8f5e9;
      border: 1px solid #3d6b1f;
      border-radius: 8px;
      color: #1b5e20;
      margin-top: 1rem;
    }

    .spinner {
      width: 20px;
      height: 20px;
      border: 3px solid #e8f5e9;
      border-top-color: #3d6b1f;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .voice-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 150px;
      color: #999;
      font-size: 1.1rem;
    }

    .voice-controls {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .btn-record {
      padding: 1rem 3rem;
      background: #e53935;
      color: white;
      border: none;
      border-radius: 50px;
      cursor: pointer;
      font-weight: 600;
      font-size: 1.1rem;
      transition: all 0.3s;
    }

    .btn-record:hover:not(:disabled) {
      background: #c62828;
      transform: scale(1.05);
    }

    .btn-record.recording {
      background: #b71c1c;
      animation: pulse-record 1.5s infinite;
    }

    .btn-record:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    @keyframes pulse-record {
      0%, 100% { box-shadow: 0 0 0 0 rgba(229, 57, 53, 0.4); }
      50% { box-shadow: 0 0 0 15px rgba(229, 57, 53, 0); }
    }

    .recording-status {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: #e53935;
      font-weight: 600;
    }

    .pulse {
      width: 12px;
      height: 12px;
      background: #e53935;
      border-radius: 50%;
      animation: pulse-dot 1s infinite;
    }

    @keyframes pulse-dot {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(0.8); }
    }

    .audio-preview {
      margin-top: 1rem;
      padding: 1rem;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .audio-preview p {
      margin: 0 0 0.5rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .audio-preview audio {
      width: 100%;
    }

    @media (max-width: 768px) {
      .voice-agent-container {
        padding: 1rem;
        height: auto;
      }

      .voice-header {
        flex-direction: column;
        gap: 1rem;
      }

      .mode-toggle {
        flex-direction: column;
      }

      .toggle-btn {
        width: 100%;
      }

      .user-message,
      .system-message {
        max-width: 100%;
      }

      .messages-container {
        max-height: 300px;
      }
    }
  `]
})
export class VoiceAgentComponent implements OnInit {
  selectedLanguage = 'en';
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

  constructor(
    private toastr: ToastrService,
    private voiceAssistantService: VoiceAssistantService,
    private languageService: LanguageService
  ) { }

  ngOnInit(): void {
    this.loadConversationHistory();
    this.languageService.getLanguages().subscribe({
      next: (response) => { this.languages = response.languages; },
      error: () => { /* fallback already handled inside LanguageService */ }
    });
  }

  onLanguageChange(): void {
    console.log('Language changed to:', this.selectedLanguage);
  }

  switchMode(mode: 'text' | 'voice'): void {
    this.inputMode = mode;
    this.textInput = '';
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

    this.voiceAssistantService.askQuestionWithAudio(audioBlob).subscribe({
      next: (response) => {
        this.lastRecordingUrl = null;
        this.isProcessing = false;

        if (response && response.success && response.answer) {
          // Use transcribed text if available (handle both snake_case and camelCase)
          const userText = response.transcribed_text || response.transcribedText || 'Voice input';

          const message: ConversationMessage = {
            timestamp: new Date(),
            userText: userText,
            systemResponse: response.answer,
            systemAudioBase64: response.audio
          };

          this.conversationHistory.push(message);
          this.saveConversationHistory();

          console.log(`Transcribed: ${userText}`);
          console.log(`Answer: ${response.answer}`);

          this.toastr.success('Voice response received');

          // Auto-play audio response if available
          if (response.audio) {
            console.log('Auto-playing AI audio response...');
            setTimeout(() => this.playAudio(response.audio), 500);
          }
        } else {
          this.toastr.error('Could not process voice input');
        }
      },
      error: (error) => {
        this.lastRecordingUrl = null;
        this.isProcessing = false;
        console.error('Voice processing error:', error);
        this.toastr.error('Failed to process voice input');
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

    console.log('Sending query to Voice Assistant:', userText);

    this.voiceAssistantService.askQuestion(userText).subscribe({
      next: (response) => {
        this.isProcessing = false;
        console.log('Voice Assistant Response:', response);

        if (response && response.success && response.answer) {
          const message: ConversationMessage = {
            timestamp: new Date(),
            userText,
            systemResponse: response.answer
          };
          this.conversationHistory.push(message);
          this.saveConversationHistory();
          this.toastr.success('Answer generated successfully');
        } else {
          console.warn('Invalid response structure:', response);
          this.toastr.error('No answer received', 'Assistant Error');
        }
      },
      error: (error) => {
        this.isProcessing = false;
        console.error('Error querying Voice Assistant:', error);
        console.error('Error status:', error.status);
        console.error('Error response:', error.error);
        this.toastr.error('Failed to connect to Voice Assistant service', 'Connection Error');

        this.conversationHistory.push({
          timestamp: new Date(),
          userText,
          systemResponse: 'Sorry, I am unable to connect to the Voice Assistant service right now. Please try again later.'
        });
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
        this.conversationHistory = JSON.parse(saved);
      } catch (e) {
        console.error('Failed to parse history', e);
      }
    }
  }
}
