import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface VoiceAssistantRequest {
  question: string;
}

export interface VoiceAssistantResponse {
  success: boolean;
  answer: string;
  status_code: number;
}

export interface VoiceWithAudioResponse {
  success: boolean;
  answer?: string;
  text?: string;
  transcribed_text?: string;
  transcribedText?: string;
  audio: string;
  language?: string;
  status_code?: number;
}

@Injectable({
  providedIn: 'root'
})
export class VoiceAssistantService {
  private apiUrl = '/api/ml/ask-question';

  constructor(private http: HttpClient) { }

  askQuestion(question: string): Observable<VoiceAssistantResponse> {
    const payload: VoiceAssistantRequest = {
      question
    };

    return this.http.post<VoiceAssistantResponse>(this.apiUrl, payload).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('HTTP Error:', error);
        console.error('Error Status:', error.status);
        console.error('Error Body:', error.error);
        return throwError(() => error);
      })
    );
  }

  askQuestionWithAudio(audioBlob: Blob): Observable<VoiceWithAudioResponse> {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.wav');

    return this.http.post<VoiceWithAudioResponse>('/api/ml/ask-question-audio', formData).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Audio Processing Error:', error);
        return throwError(() => error);
      })
    );
  }
}
