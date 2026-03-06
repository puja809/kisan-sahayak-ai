# Indian Farmer Assistance Frontend

The frontend for the Indian Farmer Assistance Application is a comprehensive, multilingual, mobile-first web interface designed to empower farmers with real-time agricultural intelligence.

## 🚀 Overview

- **Technology Stack:** Angular 17, TypeScript, RxJS
- **Primary Purpose:** Provide an intuitive and accessible interface for farmers to access all system services.

## 🛠️ Key Features

- **Multilingual Support:** Support for over 10 Indian languages (Hindi, Tamil, Telugu, etc.) using `ngx-translate`.
- **Responsive Design:** Optimized for mobile devices to ensure accessibility for farmers in the field.
- **Data Visualization:** Interactive charts and graphs for market trends and yield predictions using `Chart.js`.
- **Integrated Voice Assistant:** Voice-based interactions for easier navigation and assistance.
- **API Documentation:** Integrated Swagger UI for developers.

## 📋 Project Structure

- `src/app/pages/`: Feature-specific components (Crop, Weather, Mandi, etc.)
- `src/app/services/`: API client services for backend communication
- `src/assets/i18n/`: Translation files for multilingual support
- `src/environments/`: Configuration for development and production environments

## 🛠️ Setup and Build

### Prerequisites
- Node.js 18+
- npm 9+

### Quick Start
```bash
# Install dependencies
npm install --legacy-peer-deps

# Start development server
npm start
```
The application will be available at `http://localhost:4200`.

### Build for Production
```bash
npm run build:prod
```
The output will be in the `dist/` directory.

## 📚 Further Reading

For more information, refer to the root [README.md](../README.md).
