import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-mandi',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mandi-container">
      <h2>Mandi Prices</h2>
      <p class="loading">Loading mandi prices...</p>
    </div>
  `,
  styles: [`
    .mandi-container {
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
export class MandiComponent {}