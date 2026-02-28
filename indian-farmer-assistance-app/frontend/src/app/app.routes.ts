import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/auth/login.component').then(m => m.LoginComponent),
    title: 'Login',
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/auth/register.component').then(m => m.RegisterComponent),
    title: 'Register',
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
    title: 'Dashboard',
  },
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
    path: 'voice',
    loadComponent: () => import('./pages/voice/voice-agent.component').then(m => m.VoiceAgentComponent),
    title: 'Voice Assistant',
  },
  {
    path: 'disease',
    loadComponent: () => import('./pages/disease/disease-detection.component').then(m => m.DiseaseDetectionComponent),
    title: 'Disease Detection',
  },
  {
    path: 'iot',
    loadComponent: () => import('./pages/iot/iot-dashboard.component').then(m => m.IoTDashboardComponent),
    canActivate: [AuthGuard],
    title: 'IoT Devices',
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [AdminGuard],
    title: 'Admin Dashboard',
  },
  {
    path: 'api-docs',
    loadComponent: () => import('./pages/api-docs/api-docs.component').then(m => m.ApiDocsComponent),
    title: 'API Documentation',
  },
  {
    path: '**',
    redirectTo: '',
  },
];