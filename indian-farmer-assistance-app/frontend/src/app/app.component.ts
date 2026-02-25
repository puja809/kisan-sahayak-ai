import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div class="app-container">
      <header class="app-header">
        <div class="header-content">
          <h1 class="app-title">Indian Farmer Assistance</h1>
          <nav class="app-nav">
            <a routerLink="/" class="nav-link">Home</a>
            <a routerLink="/weather" class="nav-link">Weather</a>
            <a routerLink="/crops" class="nav-link">Crops</a>
            <a routerLink="/schemes" class="nav-link">Schemes</a>
            <a routerLink="/mandi" class="nav-link">Mandi Prices</a>
          </nav>
        </div>
      </header>
      <main class="app-main">
        <router-outlet></router-outlet>
      </main>
      <footer class="app-footer">
        <p>&copy; 2024 Indian Farmer Assistance. All rights reserved.</p>
      </footer>
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    
    .app-header {
      background-color: #2E7D32;
      color: white;
      padding: 1rem;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }
    
    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .app-title {
      font-size: 1.5rem;
      font-weight: 500;
      margin: 0;
    }
    
    .app-nav {
      display: flex;
      gap: 1rem;
    }
    
    .nav-link {
      color: white;
      text-decoration: none;
      padding: 0.5rem 1rem;
      border-radius: 4px;
      transition: background-color 0.2s;
      
      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
      }
    }
    
    .app-main {
      flex: 1;
      padding: 2rem 1rem;
      max-width: 1200px;
      margin: 0 auto;
      width: 100%;
    }
    
    .app-footer {
      background-color: #f5f5f5;
      padding: 1rem;
      text-align: center;
      color: #757575;
      font-size: 0.875rem;
    }
    
    @media (max-width: 768px) {
      .header-content {
        flex-direction: column;
        gap: 1rem;
      }
      
      .app-nav {
        flex-wrap: wrap;
        justify-content: center;
      }
    }
  `]
})
export class AppComponent {
  title = 'Indian Farmer Assistance';
}