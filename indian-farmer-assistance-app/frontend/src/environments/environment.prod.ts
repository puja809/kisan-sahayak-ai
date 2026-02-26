export const environment = {
  production: true,
  apiUrl: '/api/v1',
  wsUrl: '/ws',
  appVersion: '1.0.0',
  services: {
    admin: '/api/v1/admin',
    user: '/api/v1/user',
    crop: '/api/v1/crop',
    iot: '/api/v1/iot',
    location: '/api/v1/location',
    mandi: '/api/v1/mandi',
    scheme: '/api/v1/scheme',
    weather: '/api/v1/weather',
    yield: '/api/v1/yield',
    sync: '/api/v1/sync',
    bandwidth: '/api/v1/bandwidth',
    ai: 'http://localhost:8000', // Assuming Krishi RAG doesn't run tightly integrated behind same API gateway yet in prod, adjust as needed or use '/api/v1/ai'
  },
  supportedLanguages: [
    { code: 'en', name: 'English' },
    { code: 'hi', name: 'हिंदी (Hindi)' },
    { code: 'ta', name: 'தமிழ் (Tamil)' },
    { code: 'te', name: 'తెలుగు (Telugu)' },
    { code: 'bn', name: 'বাংলা (Bengali)' },
    { code: 'mr', name: 'मराठी (Marathi)' },
    { code: 'gu', name: 'ગુજરાતી (Gujarati)' },
    { code: 'kn', name: 'ಕನ್ನಡ (Kannada)' },
    { code: 'ml', name: 'മലയാളം (Malayalam)' },
    { code: 'pa', name: 'ਪੰਜਾਬੀ (Punjabi)' },
  ],
  defaultLanguage: 'en',
  cacheTimeout: {
    weather: 30 * 60 * 1000, // 30 minutes
    schemes: 24 * 60 * 60 * 1000, // 24 hours
    prices: 60 * 60 * 1000, // 1 hour
  },
};