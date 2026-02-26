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
    // Order matters: more-specific prefixes first
    private readonly routeMap: { prefix: string; baseUrl: string }[] = [
        { prefix: '/api/v1/admin', baseUrl: environment.services.admin },
        { prefix: '/api/v1/auth', baseUrl: environment.services.user },
        { prefix: '/api/v1/users', baseUrl: environment.services.user },
        { prefix: '/api/v1/crops/yield', baseUrl: environment.services.yield },
        { prefix: '/api/v1/ai/yield', baseUrl: environment.services.yield },
        { prefix: '/api/v1/yield', baseUrl: environment.services.yield },
        { prefix: '/api/v1/crops', baseUrl: environment.services.crop },
        { prefix: '/api/v1/iot', baseUrl: environment.services.iot },
        { prefix: '/api/v1/location', baseUrl: environment.services.location },
        { prefix: '/api/v1/fertilizer', baseUrl: environment.services.mandi },
        { prefix: '/api/v1/mandi', baseUrl: environment.services.mandi },
        { prefix: '/api/v1/schemes', baseUrl: environment.services.scheme },
        { prefix: '/api/v1/weather', baseUrl: environment.services.weather },
        { prefix: '/api/v1/sync', baseUrl: environment.services.sync },
        { prefix: '/api/v1/bandwidth', baseUrl: environment.services.bandwidth },
        { prefix: '/api/v1/ai', baseUrl: environment.services.ai }
    ];

    intercept(
        request: HttpRequest<unknown>,
        next: HttpHandler
    ): Observable<HttpEvent<unknown>> {
        // Only intercept relative /api/v1 paths (skip already-absolute URLs)
        if (!request.url.startsWith('/api/v1')) {
            return next.handle(request);
        }

        for (const route of this.routeMap) {
            if (request.url.startsWith(route.prefix)) {
                const rewrittenUrl = `${route.baseUrl}${request.url}`;
                const cloned = request.clone({ url: rewrittenUrl });
                return next.handle(cloned);
            }
        }

        // Fallback: send to the main gateway
        const fallbackUrl = `${environment.apiUrl.replace('/api/v1', '')}${request.url}`;
        return next.handle(request.clone({ url: fallbackUrl }));
    }
}
