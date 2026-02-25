# Implementation Plan: Indian Farmer Assistance Application

## Overview

This implementation plan breaks down the Indian Farmer Assistance Application into discrete, incremental coding tasks. The application is built using a microservices architecture with Angular frontend, Spring Boot/Java backend services, Python AI/ML services, MySQL for structured data, and MongoDB for vector embeddings. Each task builds on previous steps, with property-based tests and unit tests integrated throughout to validate correctness early.

## Tasks

- [x] 1. Set up project infrastructure and core configuration
  - Initialize Spring Boot microservices project structure with Maven/Gradle
  - Set up Angular workspace with required dependencies
  - Configure MySQL and MongoDB connections with connection pooling
  - Set up Redis cache configuration
  - Create Docker Compose file for local development environment
  - Configure environment-specific properties files (dev, staging, prod)
  - Set up logging framework with structured JSON logging
  - _Requirements: 23.1, 23.2, 23.3, 23.4, 23.5, 23.8_

- [x] 2. Implement authentication and user management
  - [x] 2.1 Create User entity and repository with MySQL schema
    - Define Users table with all fields (farmer_id, aadhaar_hash, name, phone, email, etc.)
    - Implement UserRepository with Spring Data JPA
    - Add indexes for farmer_id, phone, and location fields
    - _Requirements: 11.1, 11A.1, 22.1_

  - [x] 2.2 Implement AgriStack UFSI integration service
    - Create AgriStackClient for UFSI API communication
    - Implement farmer authentication via UFSI
    - Implement profile retrieval from three core registries (Farmer Registry, Geo-Referenced Village Map, Crop Sown Registry)
    - Handle authentication failures and service unavailability
    - _Requirements: 11.1, 11.2, 11.3, 11.9_

  - [x]* 2.3 Write property test for AgriStack profile retrieval
    - **Property 20: AgriStack Profile Retrieval**
    - **Validates: Requirements 11.3**

  - [x] 2.4 Implement JWT-based session management
    - Generate JWT tokens on successful authentication
    - Implement token validation and refresh logic
    - Handle session expiration with re-authentication prompt
    - _Requirements: 11.6_

  - [x]* 2.5 Write unit tests for authentication flows
    - Test successful authentication
    - Test authentication failures
    - Test session expiration
    - Test offline authentication with cached credentials
    - _Requirements: 11.6, 11.7, 11.8_

- [x] 3. Implement role-based access control (RBAC)
  - [x] 3.1 Create role management system
    - Add role field to User entity (FARMER, ADMIN)
    - Implement role assignment logic with super-admin approval for admins
    - Create audit logging for role modifications
    - _Requirements: 22.1, 22.2, 22.7_

  - [x] 3.2 Implement authorization filters and interceptors
    - Create Spring Security configuration with role-based access
    - Implement method-level security annotations
    - Add authorization checks for admin-only endpoints
    - Return 403 Forbidden for unauthorized access attempts
    - _Requirements: 22.3, 22.4, 22.5, 22.6_

  - [x]* 3.3 Write property test for role-based access control
    - **Property 41: Role-Based Access Control**
    - **Validates: Requirements 22.5**

  - [x]* 3.4 Write unit tests for RBAC
    - Test farmer access to allowed endpoints
    - Test farmer denied access to admin endpoints
    - Test admin access to all endpoints
    - Test audit logging for unauthorized attempts
    - _Requirements: 22.5, 22.7_

- [x] 4. Implement farmer profile management
  - [x] 4.1 Create farm and crop entities with relationships
    - Define Farms table with foreign key to Users
    - Define Crops table with foreign key to Farms
    - Define Fertilizer_Applications table with foreign key to Crops
    - Define Livestock and Equipment tables
    - Implement repositories for all entities
    - _Requirements: 11A.2, 11A.3, 11A.4, 11A.11, 11A.12_

  - [x] 4.2 Implement profile CRUD operations
    - Create endpoints for profile creation and updates
    - Implement crop record management (add, update, delete)
    - Implement harvest data recording
    - Implement fertilizer application tracking
    - Implement equipment and livestock management
    - Add version history tracking for profile updates
    - _Requirements: 11A.4, 11A.5, 11A.6, 11A.7, 11A.10, 11A.11, 11A.12_

  - [x]* 4.3 Write property test for profile data persistence round trip
    - **Property 21: Profile Data Persistence Round Trip**
    - **Validates: Requirements 11A.4, 11A.6**

  - [x]* 4.4 Write property test for version history maintenance
    - **Property 23: Version History Maintenance**
    - **Validates: Requirements 11A.7, 21.6**

  - [x] 4.5 Implement profile dashboard aggregation
    - Create dashboard endpoint with current crops, upcoming activities, financial summary
    - Calculate input costs vs revenue
    - Display yield predictions and variance tracking
    - _Requirements: 11A.8_

  - [x]* 4.6 Write unit tests for profile management
    - Test profile creation with all required fields
    - Test profile updates and version tracking
    - Test crop record CRUD operations
    - Test dashboard data aggregation
    - _Requirements: 11A.1, 11A.7, 11A.8_

- [-] 5. Checkpoint - Ensure authentication and profile management tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 6. Implement weather service with IMD API integration
  - [x] 6.1 Create IMD API client
    - Implement HTTP client for IMD Weather API
    - Create methods for 7-day forecast, current weather, nowcast, alerts, rainfall stats, agromet advisories
    - Implement retry logic with exponential backoff (1s, 2s, 4s, max 3 retries)
    - Handle API timeouts and rate limiting
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_

  - [x]* 6.2 Write property test for API parameter correctness
    - **Property 1: API Parameter Correctness**
    - **Validates: Requirements 1.1**

  - [x] 6.3 Implement weather data caching
    - Create Weather_Cache table in MySQL
    - Implement cache-aside pattern with Redis for hot data
    - Set cache TTL to 30 minutes for weather data
    - Implement cache invalidation on new data fetch
    - _Requirements: 1.9, 12.2_

  - [x] 6.4 Create weather service endpoints
    - GET /api/v1/weather/forecast/{district}
    - GET /api/v1/weather/current/{district}
    - GET /api/v1/weather/nowcast/{district}
    - GET /api/v1/weather/alerts/{district}
    - GET /api/v1/weather/rainfall/{district}
    - GET /api/v1/weather/agromet/{district}
    - Implement offline mode with cached data display
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

  - [x]* 6.5 Write property test for weather data completeness
    - **Property 2: Weather Data Completeness**
    - **Validates: Requirements 1.2**

  - [x]* 6.6 Write property test for alert display consistency
    - **Property 3: Alert Display Consistency**
    - **Validates: Requirements 1.4, 1.5**

  - [x]* 6.7 Write property test for cached data timestamp display
    - **Property 4: Cached Data Timestamp Display**
    - **Validates: Requirements 1.9, 5.12, 6.11, 12.5**

  - [x]* 6.8 Write unit tests for weather service
    - Test successful API calls with mock responses
    - Test API failures and fallback to cache
    - Test retry logic with exponential backoff
    - Test offline mode with cached data
    - _Requirements: 1.9, 1.10, 19.2_

- [-] 7. Implement crop recommendation service
  - [x] 7.1 Create agro-ecological zone mapping service
    - Implement GPS to agro-ecological zone mapping using ICAR classification
    - Create lookup table or API integration for zone determination
    - _Requirements: 2.1_

  - [x]* 7.2 Write property test for location to zone mapping consistency
    - **Property 5: Location to Zone Mapping Consistency**
    - **Validates: Requirements 2.1**

  - [x] 7.3 Integrate GAEZ v4 framework for crop suitability
    - Create GAEZ API client or data import service
    - Implement suitability scoring considering land, water, soil, terrain, climate
    - Incorporate soil health card data when available
    - _Requirements: 2.2, 2.3, 2.4_

  - [x] 7.4 Implement crop recommendation engine
    - Create recommendation algorithm ranking crops by suitability score
    - Consider rain-fed vs irrigated conditions
    - Integrate market data for market-linked recommendations
    - Flag crops with high climate risk
    - Include state-released seed varieties
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 2.9_

  - [x]* 7.5 Write property test for descending ranking order
    - **Property 6: Descending Ranking Order**
    - **Validates: Requirements 2.5, 3.9, 9.7, 11D.3, 21.8**

  - [x]* 7.6 Write property test for crop recommendation data completeness
    - **Property 7: Crop Recommendation Data Completeness**
    - **Validates: Requirements 2.6**

  - [x] 7.7 Create crop recommendation endpoints
    - GET /api/v1/crops/recommendations
    - POST /api/v1/crops/recommendations/calculate
    - GET /api/v1/crops/varieties/{cropName}
    - GET /api/v1/crops/suitability/{location}
    - _Requirements: 2.1, 2.2, 2.5, 2.6, 2.9, 2.10_

  - [x]* 7.8 Write unit tests for crop recommendations
    - Test zone mapping with various GPS coordinates
    - Test suitability scoring with different soil types
    - Test ranking algorithm
    - Test location change updates
    - _Requirements: 2.1, 2.5, 2.10_

- [x] 8. Implement crop rotation recommendation service
  - [x] 8.1 Create crop history analyzer
    - Implement logic to analyze cropping patterns for past 3 seasons
    - Identify nutrient depletion risks based on crop families
    - Detect consecutive planting of same crop family
    - _Requirements: 3.1, 3.2_

  - [x]* 8.2 Write property test for nutrient depletion risk detection
    - **Property 8: Nutrient Depletion Risk Detection**
    - **Validates: Requirements 3.2**

  - [x] 8.3 Implement rotation recommendation engine
    - Create algorithm for nutrient cycling optimization (deep-rooted vs shallow-rooted)
    - Implement legume integration recommendations for nitrogen fixation
    - Add rice-based system diversification logic
    - Implement intercropping and relay cropping suggestions
    - Assess pest and disease carryover risk
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 8.4 Create rotation ranking and display logic
    - Rank rotation options by soil health benefit, climate resilience, economic viability
    - Generate season-wise planting schedules (Kharif, Rabi, Zaid)
    - Include residue management recommendations
    - Provide default patterns for farmers with no crop history
    - _Requirements: 3.8, 3.9, 3.10, 3.11_

  - [x] 8.5 Create crop rotation endpoints
    - GET /api/v1/crops/rotation/{farmerId}
    - POST /api/v1/crops/rotation/generate
    - _Requirements: 3.1, 3.9, 3.10, 3.11_

  - [x]* 8.6 Write unit tests for crop rotation
    - Test crop history analysis
    - Test nutrient depletion detection
    - Test rotation ranking algorithm
    - Test default patterns for new farmers
    - _Requirements: 3.1, 3.2, 3.9, 3.11_

- [ ] 9. Checkpoint - Ensure weather and crop services tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 10. Implement government schemes service
  - [x] 10.1 Create Schemes and Scheme_Applications entities
    - Define Schemes table with all fields (scheme_code, name, type, state, eligibility, benefits, etc.)
    - Define Scheme_Applications table with status tracking
    - Implement repositories with appropriate indexes
    - _Requirements: 4.1, 4.2, 11D.10_

  - [x] 10.2 Implement scheme catalog management
    - Create endpoints for scheme CRUD operations (admin only)
    - Implement scheme categorization (central, state, crop-specific, insurance, subsidy, welfare)
    - Add state-specific filtering logic
    - _Requirements: 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

  - [x]* 10.3 Write property test for state-based scheme filtering
    - **Property 10: State-Based Scheme Filtering**
    - **Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9**

  - [x] 10.4 Implement eligibility assessment engine
    - Create eligibility checker using farmer profile data (landholding, crop, demographics)
    - Pre-assess eligibility before displaying schemes
    - Generate confidence indicators (high, medium, low)
    - _Requirements: 4.4, 4.5, 11D.1, 11D.2_

  - [x]* 10.5 Write property test for eligibility assessment consistency
    - **Property 9: Eligibility Assessment Consistency**
    - **Validates: Requirements 4.4, 11D.1**

  - [x] 10.6 Implement personalized scheme recommendations
    - Analyze farmer profile for all scheme eligibility
    - Rank schemes by benefit amount and deadline proximity
    - Highlight crop-specific schemes for farmer's crops
    - Prioritize schemes for small/marginal farmers
    - _Requirements: 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8_

  - [x] 10.7 Implement scheme application tracking
    - Create application submission endpoints
    - Track application status (draft, submitted, under review, approved, rejected, disbursed)
    - Implement status update notifications
    - _Requirements: 11D.10_

  - [-] 10.8 Implement deadline notification system
    - Create scheduled job to check approaching deadlines
    - Send push notifications 7 days and 1 day before deadline
    - _Requirements: 4.8, 11D.9_

  - [ ]* 10.9 Write property test for scheme deadline notification timing
    - **Property 28: Scheme Deadline Notification Timing**
    - **Validates: Requirements 11D.9**

  - [ ] 10.10 Create scheme service endpoints
    - GET /api/v1/schemes
    - GET /api/v1/schemes/{id}
    - GET /api/v1/schemes/crop/{cropName}
    - GET /api/v1/schemes/state/{state}
    - POST /api/v1/schemes/eligibility/check
    - GET /api/v1/schemes/personalized/{farmerId}
    - POST /api/v1/schemes/applications
    - GET /api/v1/schemes/applications/{farmerId}
    - PUT /api/v1/schemes/applications/{id}/status
    - _Requirements: 4.1, 4.2, 4.4, 4.6, 11D.1, 11D.10_

  - [ ]* 10.11 Write unit tests for scheme service
    - Test scheme filtering by state and crop
    - Test eligibility assessment with various profiles
    - Test personalized recommendations ranking
    - Test application tracking and status updates
    - Test deadline notifications
    - _Requirements: 4.3, 4.4, 4.8, 11D.3, 11D.9, 11D.10_

- [-] 11. Implement mandi price service with AGMARKNET integration
  - [ ] 11.1 Create AGMARKNET API client
    - Implement HTTP client for AGMARKNET API
    - Create methods for commodity price retrieval
    - Handle API timeouts and data unavailability
    - _Requirements: 6.1, 6.2, 6.3_

  - [ ] 11.2 Create Mandi_Prices entity and caching
    - Define Mandi_Prices table with unique constraint on (commodity, variety, mandi_code, date)
    - Implement repository with indexes on commodity, mandi, location, date
    - Set up Redis cache with 1-hour TTL for price data
    - _Requirements: 6.1, 6.2, 6.11_

  - [ ]* 11.3 Write property test for price data completeness and constraints
    - **Property 12: Price Data Completeness and Constraints**
    - **Validates: Requirements 6.2**

  - [ ] 11.4 Implement mandi location and distance sorting
    - Create mandi location service with geo-location data
    - Implement distance calculation from farmer's location
    - Sort mandis by distance in ascending order
    - _Requirements: 6.4_

  - [ ]* 11.5 Write property test for distance-based ascending sort
    - **Property 11: Distance-Based Ascending Sort**
    - **Validates: Requirements 6.4, 7.2**

  - [ ] 11.6 Implement price trend analysis
    - Create service to calculate 30-day price history
    - Generate graphical visualization data (time-series)
    - Implement MSP vs market price comparison
    - Provide post-harvest storage advisory (hold vs sell)
    - _Requirements: 6.5, 6.6, 6.7_

  - [ ] 11.7 Implement price alert system
    - Create price alert subscription endpoints
    - Implement scheduled job to check price peaks in neighboring districts
    - Send push notifications for crop price alerts
    - _Requirements: 6.10_

  - [ ] 11.8 Create mandi service endpoints
    - GET /api/v1/mandi/prices/{commodity}
    - GET /api/v1/mandi/prices/nearby
    - GET /api/v1/mandi/prices/trends/{commodity}
    - GET /api/v1/mandi/prices/msp/{commodity}
    - GET /api/v1/mandi/locations/nearby
    - POST /api/v1/mandi/alerts/subscribe
    - GET /api/v1/mandi/alerts/{farmerId}
    - _Requirements: 6.1, 6.4, 6.5, 6.6, 6.9, 6.10_

  - [ ]* 11.9 Write unit tests for mandi service
    - Test AGMARKNET API integration with mock responses
    - Test price data caching and retrieval
    - Test distance sorting
    - Test price trend calculations
    - Test alert generation and notifications
    - _Requirements: 6.1, 6.4, 6.5, 6.10, 6.11_

- [-] 12. Implement location services
  - [ ] 12.1 Create GPS location service
    - Implement GPS coordinate retrieval
    - Implement reverse geocoding to determine district, state, agro-ecological zone
    - Handle location permission requests
    - Implement fallback to network-based location or manual selection
    - _Requirements: 14.1, 14.2, 14.3, 14.5_

  - [ ]* 12.2 Write property test for reverse geocoding accuracy
    - **Property 32: Reverse Geocoding Accuracy**
    - **Validates: Requirements 14.3**

  - [ ] 12.3 Implement location change detection
    - Monitor location changes with >10km threshold
    - Update location-dependent information automatically
    - _Requirements: 14.4_

  - [ ] 12.4 Create government body locator service
    - Implement KVK, district agriculture office, state department lookup within 50km
    - Display contact information, specialization areas, distance
    - Provide directions integration with device map app
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 12.5 Write unit tests for location services
    - Test GPS coordinate retrieval
    - Test reverse geocoding with various coordinates
    - Test location change detection
    - Test government body lookup and distance calculation
    - _Requirements: 14.1, 14.3, 14.4, 7.2_

- [ ] 13. Checkpoint - Ensure schemes, mandi, and location services tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 14. Implement Python AI/ML service foundation
  - [x] 14.1 Set up Python Flask/FastAPI service
    - Initialize Python project with Flask or FastAPI
    - Configure CORS for cross-origin requests from Spring Boot services
    - Set up logging and error handling
    - Create health check endpoint
    - _Requirements: 23.3, 23.7_

  - [x] 14.2 Set up MongoDB vector database connection
    - Configure MongoDB connection with replica set support
    - Create Documents collection with vector embedding field (768 dimensions)
    - Create indexes for vector similarity search
    - Implement VectorStoreAdapter interface for abstraction
    - _Requirements: 21.4, 23.5, 23.10_

  - [x] 14.3 Implement document embedding generation
    - Integrate sentence-transformers or similar library for text embeddings
    - Create endpoint to generate 768-dimensional embeddings from text
    - Implement batch embedding generation for efficiency
    - _Requirements: 21.3_

  - [x]* 14.4 Write property test for vector embedding generation
    - **Property 39: Vector Embedding Generation**
    - **Validates: Requirements 21.3, 21.4**

  - [x] 14.4 Implement semantic search service
    - Create vector similarity search using cosine similarity
    - Implement ranking by similarity score in descending order
    - Add filtering by category, state, tags
    - _Requirements: 21.8_

  - [x]* 14.5 Write property test for semantic search ranking
    - **Property 40: Semantic Search Ranking**
    - **Validates: Requirements 21.8**

  - [x] 14.6 Create AI service endpoints
    - POST /api/v1/ai/embeddings/generate
    - POST /api/v1/ai/search/semantic
    - _Requirements: 21.3, 21.8_

  - [x]* 14.7 Write unit tests for embedding and search
    - Test embedding generation with various text inputs
    - Test semantic search with query embeddings
    - Test ranking by similarity
    - Test filtering by metadata
    - _Requirements: 21.3, 21.8_

- [-] 15. Implement crop disease detection service
  - [ ] 15.1 Integrate disease detection model
    - Load pre-trained model for Indian crop diseases
    - Implement image preprocessing pipeline
    - Create inference endpoint with GPU support
    - Implement model versioning and caching
    - _Requirements: 9.2_

  - [ ] 15.2 Implement image validation
    - Validate image format (JPEG, PNG)
    - Validate image size (≤10MB)
    - Check image quality (blur detection, lighting check)
    - _Requirements: 9.1, 9.9_

  - [ ]* 15.3 Write property test for image validation
    - **Property 16: Image Validation**
    - **Validates: Requirements 9.1**

  - [ ] 15.4 Implement disease detection logic
    - Run model inference on validated images
    - Extract disease name, severity level, confidence score
    - Generate visual overlays highlighting affected regions
    - Handle multiple disease detections and rank by confidence
    - _Requirements: 9.3, 9.4, 9.7_

  - [ ]* 15.5 Write property test for disease detection confidence threshold
    - **Property 17: Disease Detection Confidence Threshold**
    - **Validates: Requirements 9.8**

  - [ ] 15.6 Implement treatment recommendations
    - Create treatment recommendation database (organic and chemical options)
    - Provide preventive measures and estimated costs
    - Include application timing, dosage, safety precautions
    - Link to KVK experts for consultation
    - _Requirements: 9.5, 9.6, 9.11_

  - [ ] 15.7 Create disease detection endpoints
    - POST /api/v1/ai/disease/detect
    - Store detection results in Disease_Detections table
    - _Requirements: 9.1, 9.2, 9.3, 9.10_

  - [ ]* 15.8 Write unit tests for disease detection
    - Test image validation with various formats and sizes
    - Test model inference with sample images
    - Test confidence threshold handling
    - Test treatment recommendation retrieval
    - _Requirements: 9.1, 9.2, 9.8, 9.9_

- [x] 16. Implement yield prediction service
  - [x] 16.1 Create Yield_Predictions entity
    - Define Yield_Predictions table with min/expected/max predictions
    - Store factors considered, model version, actual yield, variance
    - Implement repository with indexes
    - _Requirements: 11B.1, 11B.7, 11B.9_

  - [x] 16.2 Implement yield estimation algorithm
    - Use crop type, variety, sowing date, area, growth stage
    - Incorporate historical yield data from farmer's past records
    - Factor in weather data (rainfall, temperature, extreme events)
    - Incorporate soil health data (N, P, K, pH)
    - Consider irrigation type and frequency
    - Adjust for pest/disease incidents
    - _Requirements: 11B.1, 11B.2, 11B.3, 11B.4, 11B.5, 11B.6_

  - [x]* 16.3 Write property test for yield estimate range validity
    - **Property 24: Yield Estimate Range Validity**
    - **Validates: Requirements 11B.7**

  - [x] 16.4 Implement yield prediction updates and notifications
    - Detect significant changes (>10% deviation)
    - Send notifications to farmers
    - Provide financial projections based on current mandi prices
    - _Requirements: 11B.8, 11B.10_

  - [x] 16.5 Implement variance tracking and model improvement
    - Compare actual harvest with predictions
    - Calculate variance (actual - predicted)
    - Use variance data to improve future predictions with ML
    - _Requirements: 11B.9_

  - [x]* 16.6 Write property test for prediction variance calculation
    - **Property 25: Prediction Variance Calculation**
    - **Validates: Requirements 11B.9**

  - [x] 16.7 Create yield prediction endpoints
    - POST /api/v1/crops/yield/estimate
    - GET /api/v1/crops/yield/history/{cropId}
    - POST /api/v1/crops/yield/actual
    - POST /api/v1/ai/yield/predict
    - POST /api/v1/ai/yield/update-model
    - _Requirements: 11B.1, 11B.7, 11B.9_

  - [x]* 16.8 Write unit tests for yield prediction
    - Test yield estimation with various input combinations
    - Test variance calculation
    - Test notification triggering
    - Test model improvement with variance data
    - _Requirements: 11B.1, 11B.8, 11B.9_

- [-] 17. Implement fertilizer recommendation service
  - [ ] 17.1 Create fertilizer recommendation engine
    - Use soil health card data to determine nutrient deficiencies
    - Provide default recommendations based on crop type, growth stage, agro-ecological zone
    - Specify fertilizer type (urea, DAP, MOP, organic compost), quantity per acre, timing
    - Generate split application schedules (basal dose, top dressing) with specific dates
    - Suggest organic alternatives (vermicompost, green manure, biofertilizers)
    - _Requirements: 11C.1, 11C.2, 11C.3, 11C.4, 11C.5_

  - [ ]* 17.2 Write property test for fertilizer recommendation completeness
    - **Property 26: Fertilizer Recommendation Completeness**
    - **Validates: Requirements 11C.3, 11C.4**

  - [ ] 17.3 Implement fertilizer tracking and analysis
    - Record fertilizer applications (type, quantity, date, cost)
    - Calculate total nutrient input (N, P, K) across all applications
    - Highlight over-application or under-application
    - Display cost trends and nutrient efficiency
    - _Requirements: 11C.6, 11C.7, 11C.8, 11C.11_

  - [ ]* 17.4 Write property test for nutrient calculation accuracy
    - **Property 27: Nutrient Calculation Accuracy**
    - **Validates: Requirements 11C.7**

  - [ ] 17.5 Implement fertilizer subsidy and price tracking
    - Display government subsidies for fertilizers
    - Track fertilizer price fluctuations
    - Notify farmers of price changes and optimal purchase timing
    - _Requirements: 11C.9, 11C.10_

  - [ ] 17.6 Create fertilizer service endpoints
    - GET /api/v1/crops/fertilizer/recommend
    - POST /api/v1/crops/fertilizer/calculate
    - GET /api/v1/crops/fertilizer/history/{cropId}
    - POST /api/v1/ai/fertilizer/recommend
    - POST /api/v1/ai/fertilizer/calculate-nutrients
    - _Requirements: 11C.1, 11C.3, 11C.6, 11C.7_

  - [ ]* 17.7 Write unit tests for fertilizer service
    - Test recommendation generation with soil data
    - Test default recommendations without soil data
    - Test nutrient calculation
    - Test over/under-application detection
    - _Requirements: 11C.1, 11C.2, 11C.7, 11C.8_

- [ ] 18. Checkpoint - Ensure AI/ML services tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 19. Implement Bhashini voice agent service
  - [ ] 19.1 Create Bhashini API client
    - Implement WebSocket client for real-time ASR streaming
    - Implement NMT (Neural Machine Translation) client
    - Implement TTS (Text-to-Speech) client
    - Implement OCR client for document text extraction
    - Handle API failures and fallbacks
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.13_

  - [ ] 19.2 Implement voice processing pipeline
    - Audio ingestion with Voice Activity Detection (VAD)
    - ASR for speech-to-text conversion
    - NMT for translation to English
    - Conversational intelligence to invoke external APIs (Agmarknet, IMD)
    - NMT for translation back to farmer's language
    - TTS for audio synthesis with streaming
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.6_

  - [ ]* 19.3 Write property test for language configuration consistency
    - **Property 13: Language Configuration Consistency**
    - **Validates: Requirements 8.1, 8.12**

  - [ ] 19.4 Implement disambiguation and error handling
    - Detect ambiguous queries and request confirmation
    - Handle noise levels and request retry
    - Implement fallback hierarchy: voice → text → cached answer
    - Handle language switching mid-session
    - _Requirements: 8.7, 8.8, 8.9, 8.12_

  - [ ]* 19.5 Write property test for disambiguation trigger
    - **Property 14: Disambiguation Trigger**
    - **Validates: Requirements 8.7**

  - [ ]* 19.6 Write property test for voice fallback hierarchy
    - **Property 15: Voice Fallback Hierarchy**
    - **Validates: Requirements 8.9**

  - [ ] 19.7 Implement low-bandwidth optimization
    - Compress audio data using optimized codecs
    - Minimize network usage for rural areas
    - _Requirements: 8.11, 13.3_

  - [ ] 19.8 Create voice agent endpoints
    - POST /api/v1/ai/voice/process
    - POST /api/v1/ai/voice/asr
    - POST /api/v1/ai/voice/tts
    - POST /api/v1/ai/voice/translate
    - POST /api/v1/ai/ocr/extract
    - Store conversations in Voice_Conversations MongoDB collection
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.13_

  - [ ]* 19.9 Write unit tests for voice agent
    - Test ASR with sample audio files
    - Test NMT with various languages
    - Test TTS audio generation
    - Test OCR text extraction
    - Test fallback mechanisms
    - Test language switching
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.9, 8.12, 8.13_

- [-] 20. Implement IoT device management service
  - [x] 20.1 Create IoT_Devices and Sensor_Readings entities
    - Define IoT_Devices table with device metadata
    - Define Sensor_Readings table with time-series data
    - Implement repositories with appropriate indexes
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 20.2 Implement device provisioning and discovery
    - Create device discovery service (local network, Bluetooth)
    - Implement secure connection establishment
    - Retrieve device capabilities
    - Support vendor-neutral provisioning lifecycle
    - _Requirements: 10.1, 10.2, 10.9_

  - [x] 20.3 Implement sensor data collection and display
    - Collect real-time sensor readings (soil moisture, temperature, humidity, pH)
    - Display sensor data on dashboard
    - Store historical data for 30 days
    - Generate graphical visualizations
    - _Requirements: 10.3, 10.5_

  - [x] 20.4 Implement alert system for threshold monitoring
    - Configure thresholds for each sensor type
    - Monitor sensor readings against thresholds
    - Generate alerts when thresholds are exceeded
    - Send notifications to farmers
    - _Requirements: 10.4_

  - [x]* 20.5 Write property test for IoT alert threshold monitoring
    - **Property 18: IoT Alert Threshold Monitoring**
    - **Validates: Requirements 10.4**

  - [x] 20.6 Implement device status monitoring
    - Track device online/offline status
    - Notify farmers when devices go offline
    - Display last known status
    - Handle firmware updates
    - _Requirements: 10.6, 10.8_

  - [x] 20.7 Implement data encryption and ownership
    - Encrypt IoT data at rest and in transit
    - Ensure farmers retain full ownership of IoT data
    - _Requirements: 10.10, 10.11_

  - [x]* 20.8 Write property test for end-to-end encryption
    - **Property 19: End-to-End Encryption**
    - **Validates: Requirements 10.11, 17.1, 17.2**

  - [x] 20.9 Create IoT service endpoints
    - GET /api/v1/iot/devices/{farmerId}
    - POST /api/v1/iot/devices/provision
    - PUT /api/v1/iot/devices/{deviceId}/config
    - DELETE /api/v1/iot/devices/{deviceId}
    - GET /api/v1/iot/devices/{deviceId}/readings
    - GET /api/v1/iot/devices/{deviceId}/readings/history
    - POST /api/v1/iot/devices/{deviceId}/alerts/config
    - GET /api/v1/iot/devices/{deviceId}/alerts
    - _Requirements: 10.1, 10.3, 10.4, 10.5, 10.6, 10.7_

  - [x]* 20.10 Write unit tests for IoT service
    - Test device provisioning and discovery
    - Test sensor data collection
    - Test threshold monitoring and alerts
    - Test device status tracking
    - Test data encryption
    - _Requirements: 10.1, 10.3, 10.4, 10.6, 10.11_

- [-] 21. Implement admin document management service
  - [ ] 21.1 Create document upload and validation
    - Validate document format (PDF, DOCX, TXT)
    - Validate document size (≤50MB)
    - Extract text content from documents
    - _Requirements: 21.2_

  - [ ]* 21.2 Write property test for document format validation
    - **Property 38: Document Format Validation**
    - **Validates: Requirements 21.2**

  - [ ] 21.3 Implement document embedding and storage
    - Generate vector embeddings for document text
    - Store documents in MongoDB Documents collection
    - Store embeddings with appropriate indexing
    - Categorize documents (schemes, guidelines, crop info, disease mgmt, market intel)
    - _Requirements: 21.3, 21.4, 21.5_

  - [ ] 21.4 Implement document versioning and history
    - Version documents on updates
    - Maintain change history with timestamp and admin identifier
    - Implement soft delete with 30-day retention
    - _Requirements: 21.6, 21.7_

  - [ ] 21.5 Implement document provenance and audit
    - Maintain metadata (source, upload date, version history, uploader)
    - Create audit logs for all document operations
    - _Requirements: 21.11_

  - [ ]* 21.6 Write property test for audit log completeness
    - **Property 42: Audit Log Completeness**
    - **Validates: Requirements 21.11, 22.7**

  - [ ] 21.7 Implement scheme administration
    - Create endpoints for scheme CRUD operations
    - Specify eligibility criteria, benefit amounts, deadlines, applicable states
    - Notify affected farmers when schemes are updated
    - _Requirements: 21.9, 21.10_

  - [ ] 21.8 Create admin service endpoints
    - POST /api/v1/admin/documents/upload
    - GET /api/v1/admin/documents
    - PUT /api/v1/admin/documents/{id}
    - DELETE /api/v1/admin/documents/{id}
    - POST /api/v1/admin/schemes
    - PUT /api/v1/admin/schemes/{id}
    - DELETE /api/v1/admin/schemes/{id}
    - GET /api/v1/admin/analytics/users
    - GET /api/v1/admin/analytics/usage
    - GET /api/v1/admin/audit/logs
    - _Requirements: 21.2, 21.5, 21.6, 21.7, 21.9, 21.11_

  - [ ]* 21.9 Write unit tests for admin service
    - Test document upload and validation
    - Test document versioning
    - Test soft delete and retention
    - Test scheme CRUD operations
    - Test audit logging
    - _Requirements: 21.2, 21.6, 21.7, 21.9, 21.11_

- [ ] 22. Checkpoint - Ensure voice, IoT, and admin services tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 23. Implement offline capability and synchronization
  - [ ] 23.1 Implement offline mode detection
    - Detect network connectivity changes
    - Automatically enable offline mode when disconnected
    - Display offline indicator to user within 2 seconds
    - _Requirements: 12.1_

  - [ ]* 23.2 Write property test for offline mode activation
    - **Property 29: Offline Mode Activation**
    - **Validates: Requirements 12.1**

  - [ ] 23.3 Implement offline data access
    - Allow access to cached weather data, crop recommendations, scheme information
    - Display timestamps indicating data freshness
    - _Requirements: 12.2, 12.5_

  - [ ] 23.4 Implement request queuing for offline operations
    - Queue requests performed offline (crop records, fertilizer applications, etc.)
    - Store queued requests in local storage
    - _Requirements: 12.3_

  - [ ] 23.5 Implement synchronization on connectivity restore
    - Detect connectivity restoration
    - Process queued requests in FIFO order
    - Update cached data automatically
    - Show progress indicators for large data sets
    - _Requirements: 12.4, 15.2, 15.6_

  - [ ]* 23.6 Write property test for sync queue FIFO processing
    - **Property 30: Sync Queue FIFO Processing**
    - **Validates: Requirements 12.4**

  - [ ] 23.7 Implement conflict resolution
    - Use most recent timestamp to resolve conflicts between devices
    - Display last sync time and pending changes
    - _Requirements: 15.3, 15.5_

  - [ ]* 23.8 Write property test for conflict resolution by timestamp
    - **Property 33: Conflict Resolution by Timestamp**
    - **Validates: Requirements 15.3**

  - [ ] 23.9 Implement retry logic with exponential backoff
    - Retry failed synchronization requests up to 3 times
    - Use exponential backoff (1s, 2s, 4s)
    - _Requirements: 15.4_

  - [ ]* 23.10 Write property test for retry logic bounds
    - **Property 36: Retry Logic Bounds**
    - **Validates: Requirements 19.2**

  - [ ]* 23.11 Write unit tests for offline and sync
    - Test offline mode activation
    - Test request queuing
    - Test FIFO processing
    - Test conflict resolution
    - Test retry logic
    - _Requirements: 12.1, 12.3, 12.4, 15.3, 15.4_

- [-] 24. Implement low-bandwidth optimization
  - [ ] 24.1 Implement bandwidth detection
    - Detect network bandwidth levels
    - Classify as low, medium, high bandwidth
    - _Requirements: 13.1_

  - [ ] 24.2 Implement adaptive quality reduction
    - Reduce image quality on low bandwidth
    - Compress data transfers
    - Prioritize text and critical information over images
    - _Requirements: 13.1, 13.2_

  - [ ]* 24.3 Write property test for adaptive quality reduction
    - **Property 31: Adaptive Quality Reduction**
    - **Validates: Requirements 13.1**

  - [ ] 24.4 Implement data usage monitoring
    - Track data usage
    - Notify farmers when usage exceeds configured limits
    - Suggest data-saving options
    - Provide WiFi-only download options for large files
    - _Requirements: 13.4, 13.5_

  - [ ] 24.5 Implement automatic quality adjustment
    - Automatically adjust content quality when network improves
    - _Requirements: 13.6_

  - [ ]* 24.6 Write unit tests for bandwidth optimization
    - Test bandwidth detection
    - Test quality reduction on low bandwidth
    - Test data usage tracking
    - Test automatic quality adjustment
    - _Requirements: 13.1, 13.5, 13.6_

- [-] 25. Implement data privacy and security features
  - [ ] 25.1 Implement TLS 1.3 encryption for data in transit
    - Configure all API endpoints to use TLS 1.3 or higher
    - Enforce HTTPS for all communications
    - _Requirements: 17.1_

  - [ ] 25.2 Implement AES-256 encryption for data at rest
    - Encrypt sensitive data in MySQL and MongoDB
    - Encrypt local storage data on mobile devices
    - _Requirements: 17.2_

  - [ ] 25.3 Implement secure session management
    - Clear sensitive data from memory on logout
    - Clear temporary storage on logout
    - _Requirements: 17.4_

  - [ ] 25.4 Implement data breach notification system
    - Detect data breaches
    - Notify affected farmers within 72 hours
    - _Requirements: 17.5_

  - [ ] 25.5 Implement data deletion
    - Remove all personal data within 30 days of deletion request
    - Retain anonymized analytics data
    - _Requirements: 17.6_

  - [ ]* 25.6 Write unit tests for security features
    - Test TLS configuration
    - Test data encryption at rest
    - Test session cleanup on logout
    - Test data deletion
    - _Requirements: 17.1, 17.2, 17.4, 17.6_

- [ ] 26. Checkpoint - Ensure offline, bandwidth, and security tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 27. Implement performance optimization
  - [ ] 27.1 Optimize application launch time
    - Implement lazy loading for Angular modules
    - Optimize bundle sizes with tree shaking
    - Implement code splitting
    - Target 3-second launch time on 2GB RAM devices
    - _Requirements: 18.1_

  - [ ] 27.2 Optimize navigation and transitions
    - Implement smooth transitions between screens (≤500ms)
    - Use virtual scrolling for long lists
    - Implement pagination for large data sets
    - _Requirements: 18.2_

  - [ ] 27.3 Implement loading indicators and cancellation
    - Display loading indicators for API calls
    - Allow cancellation for requests exceeding 5 seconds
    - _Requirements: 18.3_

  - [ ] 27.4 Optimize image processing
    - Complete disease detection analysis within 10 seconds
    - Use background processing for heavy computations
    - _Requirements: 18.4_

  - [ ] 27.5 Implement memory management
    - Clear caches on low memory
    - Remove non-essential data when memory is low
    - Ensure background tasks don't block user interactions
    - _Requirements: 18.5, 18.6_

  - [ ]* 27.6 Write performance tests
    - Test launch time on low-end devices
    - Test navigation transition times
    - Test image processing time
    - Test memory usage under load
    - _Requirements: 18.1, 18.2, 18.4, 18.5_

- [-] 28. Implement error handling and recovery
  - [ ] 28.1 Implement user-friendly error messages
    - Display errors in user's selected language
    - Avoid technical stack traces in user-facing messages
    - Suggest remediation actions
    - _Requirements: 19.1_

  - [ ]* 28.2 Write property test for error message user-friendliness
    - **Property 35: Error Message User-Friendliness**
    - **Validates: Requirements 19.1**

  - [ ] 28.3 Implement automatic retry with exponential backoff
    - Retry failed network requests up to 3 times
    - Use exponential backoff (1s, 2s, 4s)
    - _Requirements: 19.2_

  - [ ] 28.4 Implement crash recovery
    - Log error details on crashes
    - Restore previous session on restart
    - _Requirements: 19.3_

  - [ ] 28.5 Implement data corruption recovery
    - Detect data corruption
    - Attempt recovery from server backups
    - _Requirements: 19.4_

  - [ ] 28.6 Implement error reporting
    - Provide contact information for technical support
    - Collect diagnostic information with farmer's consent
    - _Requirements: 19.5, 19.6_

  - [ ]* 28.7 Write unit tests for error handling
    - Test error message formatting
    - Test retry logic
    - Test crash recovery
    - Test error reporting
    - _Requirements: 19.1, 19.2, 19.3, 19.6_

- [-] 29. Implement analytics and feedback
  - [ ] 29.1 Implement anonymized usage analytics
    - Collect usage analytics with farmer's consent
    - Ensure no PII is included without explicit consent
    - _Requirements: 20.1, 20.5_

  - [ ]* 29.2 Write property test for PII exclusion from analytics
    - **Property 37: PII Exclusion from Analytics**
    - **Validates: Requirements 20.5**

  - [ ] 29.3 Implement feedback system
    - Create in-app feedback form
    - Send feedback to development team with device and version info
    - Prompt for ratings after successful feature usage
    - _Requirements: 20.2, 20.3, 20.4_

  - [ ] 29.4 Implement opt-out mechanism
    - Allow farmers to opt out of analytics
    - Respect opt-out preference and disable all tracking
    - _Requirements: 20.6_

  - [ ]* 29.5 Write unit tests for analytics and feedback
    - Test analytics collection with consent
    - Test PII exclusion
    - Test feedback submission
    - Test opt-out mechanism
    - _Requirements: 20.1, 20.5, 20.6_

- [-] 30. Implement localization and accessibility
  - [ ] 30.1 Implement multilingual support
    - Support 22+ Indian languages via Bhashini
    - Translate all UI elements to selected language
    - Use simple vocabulary appropriate for farmers
    - _Requirements: 16.1, 16.3_

  - [ ]* 30.2 Write property test for localization completeness
    - **Property 34: Localization Completeness**
    - **Validates: Requirements 16.3**

  - [ ] 30.3 Implement visual aids and accessibility
    - Supplement text with icons, images, color coding
    - Provide audio alternatives for critical text
    - Use clear labels and examples in forms
    - Explain errors in simple terms with suggested solutions
    - _Requirements: 16.2, 16.4, 16.5, 16.6_

  - [ ]* 30.4 Write unit tests for localization
    - Test language switching
    - Test UI element translation
    - Test audio alternatives
    - Test form labels and examples
    - _Requirements: 16.3, 16.4, 16.5_

- [ ] 31. Checkpoint - Ensure performance, error handling, analytics, and localization tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 32. Implement Angular frontend components
  - [ ] 32.1 Create authentication module
    - Implement login/registration components
    - Integrate with AgriStack UFSI authentication
    - Implement session management
    - Implement offline authentication with cached credentials
    - _Requirements: 11.1, 11.2, 11.6, 11.8_

  - [ ] 32.2 Create dashboard module
    - Display farmer profile with personal details and Farmer ID
    - Show current crops overview with growth stages
    - Display upcoming activities (sowing, fertilizer application, harvest)
    - Show financial summary (input costs vs revenue)
    - Add quick actions (add crop, record harvest, check weather)
    - Display yield predictions and variance tracking
    - Show fertilizer usage summary and cost trends
    - _Requirements: 11A.8_

  - [ ] 32.3 Create weather module
    - Display 7-day forecast with all required fields
    - Show nowcast alerts and district warnings
    - Display agromet advisories
    - Implement offline weather cache display
    - _Requirements: 1.2, 1.4, 1.5, 1.8, 1.9_

  - [ ] 32.4 Create crop module
    - Display crop recommendations with suitability scores
    - Show rotation planner with nutrient cycling optimization
    - Display yield estimator with min/expected/max ranges
    - Show fertilizer calculator with split application schedules
    - Display crop history tracker (5 years or 10 cycles)
    - Implement harvest recording with quality grades
    - Track input costs (seeds, fertilizers, pesticides, labor)
    - Manage livestock (cattle, poultry, goats)
    - Track equipment (tractors, harvesters, pump sets)
    - _Requirements: 2.5, 2.6, 3.9, 3.10, 11A.4, 11A.5, 11A.6, 11A.11, 11A.12, 11B.7, 11C.4_

  - [ ] 32.5 Create scheme module
    - Display scheme browser with central and state-specific schemes
    - Show eligibility checker with confidence indicators
    - Display personalized scheme recommendations ranked by benefit
    - Implement application tracker with status updates
    - Show deadline notifications (7 days and 1 day before)
    - Implement document upload for applications
    - Add state-specific scheme filtering
    - Display PM-Kisan, PMFBY, KCC, and state scheme details
    - _Requirements: 4.1, 4.2, 4.4, 4.5, 4.6, 4.8, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 11D.3, 11D.9, 11D.10_

  - [ ] 32.6 Create mandi module
    - Implement price search functionality
    - Display trend visualization (30-day history)
    - Show mandi locator with distance sorting
    - Implement price alerts subscription
    - Display MSP comparison
    - _Requirements: 6.1, 6.2, 6.4, 6.5, 6.6, 6.9, 6.10_

  - [ ] 32.7 Create voice agent module
    - Implement voice input interface
    - Add language selector for 22+ languages
    - Display conversation history
    - Implement text fallback
    - Add audio playback controls
    - _Requirements: 8.1, 8.2, 8.4, 8.5, 8.9_

  - [ ] 32.8 Create disease detection module
    - Implement image capture/upload interface
    - Display disease identification results
    - Show treatment recommendations
    - Provide KVK expert links
    - Display detection history
    - _Requirements: 9.1, 9.3, 9.4, 9.5, 9.11_

  - [ ] 32.9 Create IoT module
    - Implement device management interface
    - Display sensor dashboard with real-time readings
    - Add alert configuration
    - Show historical trends (30 days)
    - Implement device provisioning workflow
    - _Requirements: 10.1, 10.3, 10.4, 10.5, 10.7_

  - [ ] 32.10 Create admin module
    - Implement document upload interface
    - Add scheme management CRUD interface
    - Display user analytics dashboard
    - Show system configuration panel
    - Display audit logs
    - _Requirements: 21.2, 21.5, 21.9, 21.11_

  - [ ]* 32.11 Write unit tests for frontend components
    - Test authentication flows
    - Test dashboard data display
    - Test weather module with mock data
    - Test crop module interactions
    - Test scheme filtering and display
    - Test mandi price display
    - Test voice agent interface
    - Test disease detection upload
    - Test IoT dashboard
    - Test admin interfaces
    - _Requirements: 11.1, 11A.8, 1.2, 2.5, 4.1, 6.1, 8.1, 9.1, 10.3, 21.2_

- [-] 33. Implement API Gateway with Spring Boot
  - [ ] 33.1 Create API Gateway service
    - Implement request routing to backend services
    - Add load balancing configuration
    - Implement rate limiting and throttling
    - Add request/response transformation
    - _Requirements: 23.2_

  - [ ] 33.2 Implement authentication filter
    - Validate JWT tokens on all requests
    - Extract user information from tokens
    - Propagate user context to downstream services
    - _Requirements: 11.6_

  - [ ] 33.3 Implement authorization filter
    - Check user roles for endpoint access
    - Enforce role-based access control
    - Log unauthorized access attempts
    - _Requirements: 22.3, 22.5_

  - [ ] 33.4 Implement rate limiting
    - Configure rate limits per user and endpoint
    - Return 429 Too Many Requests when limits exceeded
    - _Requirements: Non-functional requirements_

  - [ ]* 33.5 Write unit tests for API Gateway
    - Test request routing
    - Test authentication filter
    - Test authorization filter
    - Test rate limiting
    - _Requirements: 11.6, 22.5_

- [-] 34. Implement data retention and audit logging
  - [ ] 34.1 Create Audit_Logs entity and repository
    - Define Audit_Logs table with all required fields
    - Implement repository with indexes on user_id, timestamp, entity
    - _Requirements: 21.11, 22.7_

  - [ ] 34.2 Implement audit logging interceptor
    - Log all administrative actions (document upload, scheme CRUD, role modifications)
    - Capture timestamp, user_id, action, entity_type, entity_id, old_value, new_value
    - Log IP address and user agent
    - _Requirements: 21.11, 22.7_

  - [ ] 34.3 Implement data retention policy
    - Retain crop records for 5 years or 10 crop cycles (whichever is longer)
    - Retain error logs for 90 days, warn logs for 60 days, info logs for 30 days, debug logs for 7 days
    - Implement scheduled jobs for data cleanup
    - _Requirements: 11A.6_

  - [ ]* 34.4 Write property test for data retention policy
    - **Property 22: Data Retention Policy**
    - **Validates: Requirements 11A.6**

  - [ ]* 34.5 Write unit tests for audit logging
    - Test audit log creation for various actions
    - Test log completeness (all required fields)
    - Test data retention cleanup
    - _Requirements: 21.11, 22.7, 11A.6_

- [ ] 35. Checkpoint - Ensure frontend, gateway, and audit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 36. Implement Progressive Web App (PWA) features
  - [ ] 36.1 Configure service worker
    - Implement service worker for offline support
    - Cache critical assets (HTML, CSS, JS, images)
    - Implement cache-first strategy for static assets
    - Implement network-first strategy for API calls with cache fallback
    - _Requirements: 12.2_

  - [ ] 36.2 Create web app manifest
    - Define app name, icons, theme colors
    - Configure display mode (standalone)
    - Set start URL and scope
    - _Requirements: 23.1_

  - [ ] 36.3 Implement push notifications
    - Configure push notification service
    - Implement notification handlers for weather alerts, price alerts, scheme deadlines
    - Request notification permissions
    - _Requirements: 4.8, 6.10, 11D.9_

  - [ ]* 36.4 Write unit tests for PWA features
    - Test service worker caching
    - Test offline functionality
    - Test push notifications
    - _Requirements: 12.2, 4.8, 6.10_

- [-] 37. Set up Docker containerization
  - [ ] 37.1 Create Dockerfiles for all services
    - Create Dockerfile for Angular frontend (Nginx)
    - Create Dockerfile for Spring Boot API Gateway
    - Create Dockerfile for Spring Boot backend services
    - Create Dockerfile for Python AI/ML service
    - _Requirements: 23.8_

  - [ ] 37.2 Create Docker Compose configuration
    - Define services for frontend, gateway, backend services, AI service
    - Define services for MySQL, MongoDB, Redis
    - Configure networking between containers
    - Set up volume mounts for persistent data
    - Configure environment variables
    - _Requirements: 23.8_

  - [ ] 37.3 Implement health check endpoints
    - Add health check endpoints to all services
    - Configure Docker health checks
    - _Requirements: 23.8_

  - [ ]* 37.4 Test Docker deployment
    - Test container builds
    - Test Docker Compose startup
    - Test inter-service communication
    - Test health checks
    - _Requirements: 23.8_

- [-] 38. Implement monitoring and logging
  - [ ] 38.1 Set up centralized logging
    - Configure structured JSON logging for all services
    - Implement log aggregation (ELK stack or similar)
    - Set up log retention policies
    - _Requirements: Non-functional requirements_

  - [ ] 38.2 Implement metrics collection
    - Add Prometheus metrics endpoints to all services
    - Collect API response times, error rates, throughput
    - Collect database query performance metrics
    - Collect cache hit rates
    - _Requirements: Non-functional requirements_

  - [ ] 38.3 Set up alerting
    - Configure alerts for critical errors
    - Configure alerts for performance degradation
    - Configure alerts for service unavailability
    - Set up notification channels (email, Slack, SMS)
    - _Requirements: Non-functional requirements_

  - [ ]* 38.4 Test monitoring and alerting
    - Test log aggregation
    - Test metrics collection
    - Test alert triggering
    - _Requirements: Non-functional requirements_

- [-] 39. Implement database migrations and seeding
  - [ ] 39.1 Create database migration scripts
    - Create Flyway or Liquibase migrations for MySQL schema
    - Create MongoDB schema initialization scripts
    - Version all migrations
    - _Requirements: 23.4_

  - [ ] 39.2 Create seed data scripts
    - Seed government schemes data (PM-Kisan, PMFBY, state schemes)
    - Seed KVK and government body locations
    - Seed crop varieties and GAEZ data
    - Seed agro-ecological zone mappings
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 7.2, 2.9_

  - [ ]* 39.3 Test database migrations
    - Test migration execution
    - Test rollback procedures
    - Test seed data loading
    - _Requirements: 23.4_

- [x] 40. Implement integration tests
  - [ ]* 40.1 Write end-to-end integration tests
    - Test farmer registration and authentication flow
    - Test weather data retrieval and caching
    - Test crop recommendation generation
    - Test scheme eligibility and application
    - Test mandi price retrieval and alerts
    - Test disease detection upload and results
    - Test IoT device provisioning and data collection
    - Test voice agent query processing
    - Test admin document upload and search
    - _Requirements: 11.1, 1.1, 2.5, 4.4, 6.1, 9.2, 10.1, 8.6, 21.3_

  - [ ]* 40.2 Write API integration tests
    - Test IMD API integration with mock server
    - Test AGMARKNET API integration with mock server
    - Test AgriStack UFSI integration with mock server
    - Test Bhashini API integration with mock server
    - Test fallback mechanisms for API failures
    - _Requirements: 1.1, 6.1, 11.1, 8.2_

- [ ] 41. Final checkpoint - Run all tests and verify system integration
  - Ensure all unit tests pass
  - Ensure all property-based tests pass
  - Ensure all integration tests pass
  - Verify Docker deployment works end-to-end
  - Verify monitoring and logging are operational
  - Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
- Property tests validate universal correctness properties with minimum 100 iterations
- Unit tests validate specific examples, edge cases, and error conditions
- The implementation follows a microservices architecture with clear separation of concerns
- All services are containerized for consistent deployment
- Offline-first design ensures critical features work without connectivity
- Multilingual support via Bhashini enables access for farmers across India
- Security and privacy are built-in from the start (TLS 1.3, AES-256, RBAC)
- The system is designed to scale to 1M+ concurrent users with 99.5% uptime
