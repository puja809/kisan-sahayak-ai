import { Injectable } from '@angular/core';
import {
    HttpRequest,
    HttpHandler,
    HttpEvent,
    HttpInterceptor,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * HTTP Interceptor that routes API calls to the correct microservice port.
 * Matches the request path prefix and rewrites the URL to
 * http://localhost:<service-port>/api/v1/...
 */
@Injectable()
export class ApiRoutingInterceptor implements HttpInterceptor {
    intercept(
        request: HttpRequest<unknown>,
        next: HttpHandler
    ): Observable<HttpEvent<unknown>> {
        // Only intercept relative /api paths (skip already-absolute URLs)
        if (!request.url.startsWith('/api')) {
            return next.handle(request);
        }

        // Clean prefix logic: prepend the environment apiUrl (e.g. 'http://localhost:8080' or '')
        const rewrittenUrl = `${environment.apiUrl}${request.url}`;
        return next.handle(request.clone({ url: rewrittenUrl }));
    }
}
