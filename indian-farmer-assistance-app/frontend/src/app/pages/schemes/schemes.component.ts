import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-schemes',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="schemes-container">
      <h2>Government Schemes</h2>
      <p class="loading">Loading government schemes...</p>
    </div>
  `,
  styles: [`
    .schemes-container {
      padding: 1rem 0;
    }
    
    h2 {
      color: #2E7D32;
      margin-bottom: 1rem;
    }
    
    .loading {
      text-align: center;
      color: #757575;
      padding: 2rem;
    }
  `]
})
export class SchemesComponent {}