export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  wsUrl: 'ws://localhost:8080/ws',
  appVersion: '1.0.0-SNAPSHOT',
  services: {
    admin: 'http://localhost:8091',
    user: 'http://localhost:8099',
    crop: 'http://localhost:8093',
    iot: 'http://localhost:8094',
    location: 'http://localhost:8095',
    mandi: 'http://localhost:8096',
    scheme: 'http://localhost:8097',
    weather: 'http://localhost:8100',
    yield: 'http://localhost:8101',
    sync: 'http://localhost:8098',
    bandwidth: 'http://localhost:8092',
    ai: 'http://localhost:8001',
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