import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { VoiceAssistantService } from '../../services/voice-assistant.service';

interface ConversationMessage {
  timestamp: Date;
  userText: string;
  systemResponse: string;
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
        <div class="header-content">
          <h1>🌾 Krishi Assistant</h1>
          <p>Ask questions about farming, schemes, and agriculture</p>
        </div>
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
          disabled
          title="Voice input coming soon"
        >
          🎤 Voice (Coming Soon)
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

        <!-- Voice Input Section (Placeholder) -->
        <div class="input-section" *ngIf="inputMode === 'voice'">
          <div class="voice-placeholder">
            <p>🎤 Voice input will be available soon</p>
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
  inputMode: 'text' | 'voice' = 'text';
  conversationHistory: ConversationMessage[] = [];
  isProcessing = false;
  textInput = '';

  constructor(
    private toastr: ToastrService,
    private voiceAssistantService: VoiceAssistantService
  ) { }

  ngOnInit(): void {
    this.loadConversationHistory();
  }

  onLanguageChange(): void {
    console.log('Language changed to:', this.selectedLanguage);
  }

  switchMode(mode: 'text' | 'voice'): void {
    this.inputMode = mode;
    this.textInput = '';
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
