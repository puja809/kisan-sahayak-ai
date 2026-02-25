import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-crops',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="crops-container">
      <h2>Crop Recommendations</h2>
      <p class="loading">Loading crop recommendations...</p>
    </div>
  `,
  styles: [`
    .crops-container {
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
export class CropsComponent {}