import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, shareReplay } from 'rxjs/operators';

export interface Language {
    code: string;
    name: string;
    nativeName: string;
}

export interface LanguagesResponse {
    languages: Language[];
    default: string;
}

// Fallback list used if the API call fails
const FALLBACK_LANGUAGES: Language[] = [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'hi', name: 'Hindi', nativeName: 'हिंदी' },
    { code: 'bn', name: 'Bengali', nativeName: 'বাংলা' },
    { code: 'te', name: 'Telugu', nativeName: 'తెలుగు' },
    { code: 'mr', name: 'Marathi', nativeName: 'मराठी' },
    { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்' },
    { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી' },
    { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ' },
    { code: 'ka', name: 'Kannada', nativeName: 'ಕನ್ನಡ' },
    { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം' },
];

@Injectable({
    providedIn: 'root'
})
export class LanguageService {
    private readonly apiUrl = '/api/ml/languages';

    // Cache the response so it's only fetched once per session
    private languages$: Observable<LanguagesResponse> | null = null;

    constructor(private http: HttpClient) { }

    getLanguages(): Observable<LanguagesResponse> {
        if (!this.languages$) {
            this.languages$ = this.http.get<LanguagesResponse>(this.apiUrl).pipe(
                catchError(() => of({ languages: FALLBACK_LANGUAGES, default: 'en' })),
                shareReplay(1)
            );
        }
        return this.languages$;
    }
}
