import { Injectable } from '@angular/core';
import { ApiDocumentationService, ServiceDocumentation, ServiceEndpoint } from './api-documentation.service';

export interface GeneratedClient {
  serviceName: string;
  className: string;
  code: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiClientGeneratorService {
  constructor(private apiDocService: ApiDocumentationService) {}

  /**
   * Generate TypeScript client code from OpenAPI documentation
   */
  generateClient(documentation: ServiceDocumentation): GeneratedClient {
    const className = this.toPascalCase(documentation.serviceName.replace(' Service', '')) + 'Client';
    const methods = this.generateMethods(documentation.endpoints, documentation.port);

    const code = `
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Auto-generated client for ${documentation.serviceName}
 * Service: ${documentation.title || documentation.serviceName}
 * Port: ${documentation.port}
 * Version: ${documentation.version || 'unknown'}
 */
@Injectable({
  providedIn: 'root'
})
export class ${className} {
  private readonly baseUrl = 'http://localhost:${documentation.port}';

  constructor(private http: HttpClient) {}

${methods}
}
`;

    return {
      serviceName: documentation.serviceName,
      className,
      code: code.trim()
    };
  }

  /**
   * Generate method code for all endpoints
   */
  private generateMethods(endpoints: ServiceEndpoint[], port: number): string {
    return endpoints
      .map(endpoint => this.generateMethod(endpoint))
      .join('\n\n  ')
      .trim();
  }

  /**
   * Generate a single method for an endpoint
   */
  private generateMethod(endpoint: ServiceEndpoint): string {
    const methodName = this.generateMethodName(endpoint);
    const httpMethod = endpoint.method.toLowerCase();
    const pathWithParams = this.replacePathParams(endpoint.path);
    const params = this.extractPathParams(endpoint.path);
    const queryParams = this.extractQueryParams(endpoint.parameters);
    const hasBody = ['post', 'put', 'patch'].includes(httpMethod);

    let signature = `${methodName}(`;
    const paramList: string[] = [];

    // Add path parameters
    params.forEach(param => {
      paramList.push(`${param}: string | number`);
    });

    // Add query parameters
    queryParams.forEach(param => {
      paramList.push(`${param}?: any`);
    });

    // Add body parameter
    if (hasBody) {
      paramList.push('body?: any');
    }

    signature += paramList.join(', ') + '): Observable<any>';

    let methodBody = `{
    let url = \`\${this.baseUrl}${pathWithParams}\`;`;

    // Add query parameters handling
    if (queryParams.length > 0) {
      methodBody += `
    let params = new HttpParams();`;
      queryParams.forEach(param => {
        methodBody += `
    if (${param} !== undefined) {
      params = params.set('${param}', ${param});
    }`;
      });
      methodBody += `
    return this.http.${httpMethod}<any>(url, ${hasBody ? 'body, ' : ''}{ params });`;
    } else {
      methodBody += `
    return this.http.${httpMethod}<any>(url${hasBody ? ', body' : ''});`;
    }

    methodBody += `
  }`;

    const comment = this.generateMethodComment(endpoint);
    return `${comment}
  ${signature} ${methodBody}`;
  }

  /**
   * Generate JSDoc comment for method
   */
  private generateMethodComment(endpoint: ServiceEndpoint): string {
    const lines: string[] = ['/**'];

    if (endpoint.summary) {
      lines.push(` * ${endpoint.summary}`);
    }

    if (endpoint.description) {
      lines.push(` * ${endpoint.description}`);
    }

    lines.push(` * @method ${endpoint.method}`);
    lines.push(` * @path ${endpoint.path}`);

    if (endpoint.parameters && endpoint.parameters.length > 0) {
      lines.push(` * @param parameters`);
      endpoint.parameters.forEach(param => {
        lines.push(` *   - ${param.name}: ${param.schema?.type || 'any'}`);
      });
    }

    lines.push(` * @returns Observable<any>`);
    lines.push(' */');

    return lines.join('\n  ');
  }

  /**
   * Generate method name from endpoint path and HTTP method
   */
  private generateMethodName(endpoint: ServiceEndpoint): string {
    const method = endpoint.method.toLowerCase();
    const pathParts = endpoint.path
      .split('/')
      .filter(p => p && !p.startsWith('{'))
      .map(p => this.toPascalCase(p));

    let name = method + pathParts.join('');

    // Handle special cases
    if (method === 'get' && endpoint.path.includes('{')) {
      name = 'get' + pathParts.join('') + 'ById';
    }

    return this.toCamelCase(name);
  }

  /**
   * Replace path parameters with template literals
   */
  private replacePathParams(path: string): string {
    return path.replace(/{([^}]+)}/g, '${$1}');
  }

  /**
   * Extract path parameters from endpoint path
   */
  private extractPathParams(path: string): string[] {
    const matches = path.match(/{([^}]+)}/g) || [];
    return matches.map(m => m.slice(1, -1));
  }

  /**
   * Extract query parameters from endpoint parameters
   */
  private extractQueryParams(parameters?: any[]): string[] {
    if (!parameters) return [];
    return parameters
      .filter(p => p.in === 'query')
      .map(p => p.name);
  }

  /**
   * Convert string to camelCase
   */
  private toCamelCase(str: string): string {
    return str.replace(/[-_](.)/g, (_, c) => c.toUpperCase())
      .replace(/^(.)/, c => c.toLowerCase());
  }

  /**
   * Convert string to PascalCase
   */
  private toPascalCase(str: string): string {
    return str
      .split(/[-_]/)
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join('');
  }

  /**
   * Export generated client as TypeScript file
   */
  exportAsFile(client: GeneratedClient): void {
    const element = document.createElement('a');
    const file = new Blob([client.code], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = `${this.toCamelCase(client.className)}.ts`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }
}
