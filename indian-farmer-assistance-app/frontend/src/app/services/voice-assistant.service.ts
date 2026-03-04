import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface VoiceAssistantRequest {
  question: string;
  language?: string;
  latitude?: number;
  longitude?: number;
  city_name?: string;
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

export interface StreamUpdate {
  type: 'start' | 'chunk' | 'done' | 'transcript' | 'audio' | 'status' | 'error';
  chunk?: string;
  text?: string;
  transcript?: string;
  audio?: string;
  language?: string;
  message?: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VoiceAssistantService {
  private apiUrl = '/api/ml/ask-question';
  private streamUrl = `${environment.apiUrl}/api/ml/ask-question/stream`;
  private audioStreamUrl = `${environment.apiUrl}/api/ml/ask-question-audio/stream`;

  constructor(private http: HttpClient) { }

  askQuestionStream(question: string, language: string = 'en', latitude?: number, longitude?: number, cityName?: string): Observable<StreamUpdate> {
    return new Observable<StreamUpdate>(observer => {
      const payload: any = { question, language };
      if (latitude !== undefined && longitude !== undefined) {
        payload.latitude = latitude;
        payload.longitude = longitude;
      }
      if (cityName) {
        payload.city_name = cityName;
      }

      fetch(this.streamUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      }).then(response => {
        if (!response.body) {
          throw new Error('ReadableStream not supported');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              observer.complete();
              return;
            }

            const chunkStr = decoder.decode(value, { stream: true });
            const lines = chunkStr.split('\n');

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                try {
                  const data = JSON.parse(line.substring(6));
                  observer.next(data);
                } catch (e) {
                  console.error('Error parsing SSE data:', e, 'Line:', line);
                }
              } else if (line.trim().length > 0 && !line.startsWith('data:') && !line.startsWith(':')) {
                // Sometime API Gateway strips SSE formatting
                try {
                  const data = JSON.parse(line.trim());
                  observer.next(data);
                } catch (e) {
                  observer.next({ type: 'chunk', chunk: line.trim() });
                }
              }
            }
            read();
          }).catch(error => {
            observer.error(error);
          });
        }
        read();
      }).catch(error => {
        console.error('Streaming request error:', error);
        observer.error(error);
      });
    });
  }

  askQuestionWithAudioStream(audioBlob: Blob, language: string = 'en', latitude?: number, longitude?: number, cityName?: string): Observable<StreamUpdate> {
    return new Observable<StreamUpdate>(observer => {
      const formData = new FormData();
      formData.append('audio', audioBlob, 'recording.wav');
      formData.append('language', language);
      if (latitude !== undefined && longitude !== undefined) {
        formData.append('latitude', latitude.toString());
        formData.append('longitude', longitude.toString());
      }
      if (cityName) {
        formData.append('city_name', cityName);
      }

      fetch(this.audioStreamUrl, {
        method: 'POST',
        body: formData
      }).then(response => {
        if (!response.body) {
          throw new Error('ReadableStream not supported');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              observer.complete();
              return;
            }

            const chunkStr = decoder.decode(value, { stream: true });
            const lines = chunkStr.split('\n');

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                try {
                  const data = JSON.parse(line.substring(6));
                  observer.next(data);
                } catch (e) {
                  console.error('Error parsing SSE Audio data:', e, 'Line:', line);
                }
              } else if (line.trim().length > 0 && !line.startsWith('data:') && !line.startsWith(':')) {
                // Try parsing raw JSON if API gateway strips "data: "
                try {
                  const data = JSON.parse(line.trim());
                  observer.next(data);
                } catch (e) {
                  observer.next({ type: 'chunk', chunk: line.trim() });
                }
              }
            }
            read();
          }).catch(error => {
            observer.error(error);
          });
        }
        read();
      }).catch(error => {
        console.error('Streaming audio request error:', error);
        observer.error(error);
      });
    });
  }

  askQuestion(question: string, language: string = 'en', latitude?: number, longitude?: number, cityName?: string): Observable<VoiceAssistantResponse> {
    const payload: VoiceAssistantRequest = { question, language };
    if (latitude !== undefined && longitude !== undefined) {
      payload.latitude = latitude;
      payload.longitude = longitude;
    }
    if (cityName) {
      payload.city_name = cityName;
    }
    return this.http.post<VoiceAssistantResponse>(this.apiUrl, payload).pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(() => error);
      })
    );
  }

  askQuestionWithAudio(audioBlob: Blob, language: string = 'en', latitude?: number, longitude?: number, cityName?: string): Observable<VoiceWithAudioResponse> {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.wav');
    formData.append('language', language);
    if (latitude !== undefined && longitude !== undefined) {
      formData.append('latitude', latitude.toString());
      formData.append('longitude', longitude.toString());
    }
    if (cityName) {
      formData.append('city_name', cityName);
    }

    return this.http.post<VoiceWithAudioResponse>('/api/ml/ask-question-audio', formData).pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(() => error);
      })
    );
  }
}
