import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent),
    title: 'Indian Farmer Assistance - Home',
  },
  {
    path: 'weather',
    loadComponent: () => import('./pages/weather/weather.component').then(m => m.WeatherComponent),
    title: 'Weather Forecast',
  },
  {
    path: 'crops',
    loadComponent: () => import('./pages/crops/crops.component').then(m => m.CropsComponent),
    title: 'Crop Recommendations',
  },
  {
    path: 'schemes',
    loadComponent: () => import('./pages/schemes/schemes.component').then(m => m.SchemesComponent),
    title: 'Government Schemes',
  },
  {
    path: 'mandi',
    loadComponent: () => import('./pages/mandi/mandi.component').then(m => m.MandiComponent),
    title: 'Mandi Prices',
  },
  {
    path: '**',
    redirectTo: '',
  },
];