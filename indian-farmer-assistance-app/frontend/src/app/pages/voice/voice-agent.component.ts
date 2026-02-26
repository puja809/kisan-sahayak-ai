import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';
import { environment } from '../../../environments/environment';

interface ConversationMessage {
  timestamp: Date;
  userText: string;
  systemResponse: string;
  sections?: string[];
  userAudioPath?: string;
  systemAudioPath?: string;
}

@Component({
  selector: 'app-voice-agent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="voice-agent-container">
      <div class="voice-header">
        <h1>Voice Assistant (Krishi RAG)</h1>
        <div class="language-selector">
          <label for="language">Language:</label>
          <select id="language" [(ngModel)]="selectedLanguage" (change)="onLanguageChange()">
            <option value="en">English</option>
            <option value="hi">Hindi</option>
            <option value="ta">Tamil</option>
            <option value="te">Telugu</option>
            <option value="ka">Kannada</option>
            <option value="ml">Malayalam</option>
            <option value="mr">Marathi</option>
            <option value="gu">Gujarati</option>
            <option value="pa">Punjabi</option>
            <option value="bn">Bengali</option>
          </select>
        </div>
      </div>

      <div class="voice-content">
        <!-- Conversation History -->
        <div class="conversation-history">
          <h2>Conversation History</h2>
          <div class="messages-container">
            <div *ngFor="let message of conversationHistory" class="message-group">
              <div class="user-message">
                <p><strong>You:</strong> {{ message.userText }}</p>
              </div>
              <div class="system-message">
                <p><strong>Assistant:</strong> {{ message.systemResponse }}</p>
                <div *ngIf="message.sections && message.sections.length > 0" class="reference-sections">
                  <small><strong>References:</strong></small>
                  <ul>
                    <li *ngFor="let section of message.sections"><small>{{ section }}</small></li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Voice Input Section -->
        <div class="voice-input-section">
          <h2>Voice Input</h2>
          <div class="voice-controls">
            <button 
              class="btn-voice" 
              [class.recording]="isRecording"
              (click)="toggleRecording()"
            >
              <span class="icon">🎤</span>
              {{ isRecording ? 'Stop Recording' : 'Start Recording' }}
            </button>
            <button 
              class="btn-secondary" 
              (click)="clearHistory()"
              [disabled]="conversationHistory.length === 0"
            >
              Clear History
            </button>
          </div>

          <div *ngIf="isRecording" class="recording-indicator">
            <div class="pulse"></div>
            <p>Recording...</p>
          </div>

          <div *ngIf="isProcessing" class="processing-indicator">
            <p>Generating response from AIF AGI...</p>
          </div>
        </div>

        <!-- Text Input Fallback -->
        <div class="text-input-section">
          <h2>Text Input (Fallback)</h2>
          <div class="text-input-group">
            <input
              type="text"
              [(ngModel)]="textInput"
              placeholder="Ask a question about AIF Scheme..."
              (keyup.enter)="sendTextQuery()"
              class="text-input"
              [disabled]="isProcessing"
            />
            <button class="btn-send" (click)="sendTextQuery()" [disabled]="!textInput.trim() || isProcessing">
              Send
            </button>
          </div>
        </div>

        <!-- Audio Playback -->
        <div class="audio-playback-section" *ngIf="lastAudioPath">
          <h2>Audio Response</h2>
          <audio controls class="audio-player">
            <source [src]="lastAudioPath" type="audio/mpeg" />
            Your browser does not support the audio element.
          </audio>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .voice-agent-container {
      padding: 1.5rem;
      max-width: 900px;
      margin: 0 auto;
    }

    .voice-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .voice-header h1 {
      font-size: 1.75rem;
      color: #333;
      margin: 0;
    }

    .language-selector {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .language-selector label {
      font-weight: 600;
      color: #333;
    }

    .language-selector select {
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .voice-content {
      display: grid;
      gap: 1.5rem;
    }

    .conversation-history {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .conversation-history h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .messages-container {
      max-height: 500px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .message-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .user-message {
      background: #e3f2fd;
      padding: 1rem;
      border-radius: 4px;
      margin-left: 2rem;
    }

    .user-message p {
      margin: 0;
      color: #333;
    }

    .system-message {
      background: #f0f4ff;
      padding: 1rem;
      border-radius: 4px;
      margin-right: 2rem;
      border-left: 4px solid #667eea;
    }

    .system-message p {
      margin: 0;
      color: #333;
      white-space: pre-wrap;
    }

    .reference-sections {
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px dashed #ccc;
    }

    .reference-sections ul {
      margin: 5px 0 0;
      padding-left: 20px;
      color: #666;
    }

    .voice-input-section {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .voice-input-section h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .voice-controls {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .btn-voice {
      flex: 1;
      padding: 1rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      transition: all 0.3s;
    }

    .btn-voice:hover {
      background: #5568d3;
    }

    .btn-voice.recording {
      background: #e74c3c;
      animation: pulse 1s infinite;
    }

    .btn-voice .icon {
      font-size: 1.5rem;
    }

    .btn-secondary {
      padding: 1rem 1.5rem;
      background: #6c757d;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }

    .btn-secondary:hover:not(:disabled) {
      background: #5a6268;
    }

    .btn-secondary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .recording-indicator {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      color: #856404;
    }

    .pulse {
      width: 12px;
      height: 12px;
      background: #e74c3c;
      border-radius: 50%;
      animation: pulse 1s infinite;
    }

    @keyframes pulse {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.5;
      }
    }

    .processing-indicator {
      padding: 1rem;
      background: #e3f2fd;
      border: 1px solid #2196f3;
      border-radius: 4px;
      color: #1976d2;
      text-align: center;
    }

    .text-input-section {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .text-input-section h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .text-input-group {
      display: flex;
      gap: 0.75rem;
    }

    .text-input {
      flex: 1;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .text-input:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
    
    .text-input:disabled {
      background-color: #f5f5f5;
    }

    .btn-send {
      padding: 0.75rem 1.5rem;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }

    .btn-send:hover:not(:disabled) {
      background: #5568d3;
    }

    .btn-send:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .audio-playback-section {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .audio-playback-section h2 {
      font-size: 1.25rem;
      color: #333;
      margin-bottom: 1rem;
      border-bottom: 2px solid #667eea;
      padding-bottom: 0.5rem;
    }

    .audio-player {
      width: 100%;
      margin-top: 1rem;
    }

    @media (max-width: 768px) {
      .voice-header {
        flex-direction: column;
        gap: 1rem;
        text-align: center;
      }

      .user-message {
        margin-left: 0;
      }

      .system-message {
        margin-right: 0;
      }

      .voice-controls {
        flex-direction: column;
      }
    }
  `]
})
export class VoiceAgentComponent implements OnInit {
  selectedLanguage = 'en';
  conversationHistory: ConversationMessage[] = [];
  isRecording = false;
  isProcessing = false;
  textInput = '';
  lastAudioPath: string | null = null;
  private ragApiUrl = environment.services.ai + '/ask';

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadConversationHistory();
  }

  onLanguageChange(): void {
    console.log('Language changed to:', this.selectedLanguage);
    // Future: handle translation before/after RAG queries
  }

  toggleRecording(): void {
    this.isRecording = !this.isRecording;
    if (this.isRecording) {
      this.startRecording();
    } else {
      this.stopRecording();
    }
  }

  private startRecording(): void {
    console.log('Starting voice recording...');
    // Implement WebRTC audio capture
  }

  private stopRecording(): void {
    console.log('Stopping voice recording...');
    this.isProcessing = true;

    // Once audio is transcribed to text, send as normal query.
    // For now we'll simulate a failure since we're using Text input primarily with the RAG
    setTimeout(() => {
      this.isProcessing = false;
      this.toastr.warning('Voice recording is currently mocked. Please use text input.', 'Demo Mode');
    }, 1500);
  }

  sendTextQuery(): void {
    if (!this.textInput.trim() || this.isProcessing) return;

    this.isProcessing = true;
    const userText = this.textInput;
    this.textInput = '';

    const payload = {
      question: userText
    };

    console.log('Sending query to Krishi RAG:', payload);

    this.http.post<any>(this.ragApiUrl, payload).subscribe({
      next: (response) => {
        this.isProcessing = false;

        if (response.success) {
          const message: ConversationMessage = {
            timestamp: new Date(),
            userText,
            systemResponse: response.answer,
            sections: response.sections
          };
          this.conversationHistory.push(message);
          this.saveConversationHistory();
          this.toastr.success('Answer generated successfully');
        } else {
          this.toastr.error(response.error || 'Failed to get an answer', 'RAG Error');
        }
      },
      error: (error) => {
        this.isProcessing = false;
        console.error('Error querying Krishi RAG:', error);
        this.toastr.error('Failed to connect to Krishi RAG service', 'Connection Error');

        // Push error message to chat for visibility
        this.conversationHistory.push({
          timestamp: new Date(),
          userText,
          systemResponse: 'Sorry, I am unable to connect to the backend AIF RAG service right now. Please make sure the krishi_rag service is running on port 8000.'
        });
        this.saveConversationHistory();
      }
    });
  }

  clearHistory(): void {
    this.conversationHistory = [];
    this.lastAudioPath = null;
    this.saveConversationHistory();
  }

  private saveConversationHistory(): void {
    // Save history to local storage, omitting the welcome message if it's the only one
    if (this.conversationHistory.length <= 1) {
      localStorage.removeItem('krishi_rag_history');
    } else {
      localStorage.setItem('krishi_rag_history', JSON.stringify(this.conversationHistory));
    }
  }

  private loadConversationHistory(): void {
    // Optionally load from local storage
    const saved = localStorage.getItem('krishi_rag_history');
    if (saved) {
      try {
        this.conversationHistory = JSON.parse(saved);
      } catch (e) {
        console.error('Failed to parse history', e);
      }
    }

    // If empty, add a welcome message
    if (this.conversationHistory.length === 0) {
      this.conversationHistory.push({
        timestamp: new Date(),
        userText: 'Hello',
        systemResponse: 'Namaste! I am the Krishi RAG Assistant. Ask me any questions about the Agriculture Infrastructure Fund (AIF) scheme.'
      });
    }
  }
}
