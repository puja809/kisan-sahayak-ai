# Indian Farmer Assistance App - System Flows

## Table of Contents
1. [User Registration Flow](#user-registration-flow)
2. [Crop Recommendation Flow](#crop-recommendation-flow)
3. [Market Price Search Flow](#market-price-search-flow)
4. [Weather Advisory Flow](#weather-advisory-flow)
5. [Disease Detection Flow](#disease-detection-flow)
6. [Voice Assistant Flow](#voice-assistant-flow)
7. [Yield Calculation Flow](#yield-calculation-flow)
8. [Government Scheme Search Flow](#government-scheme-search-flow)

---

## User Registration Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Enter email, password, name
    │
    └─→ POST /api/v1/auth/register
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate request format
            ├─ Check rate limiting
            │
            └─→ User Service (8099)
                │
                ├─ Validate email format
                ├─ Check email uniqueness
                ├─ Hash password (BCrypt)
                ├─ Create User entity
                ├─ Store in PostgreSQL
                ├─ Generate JWT token
                │
                └─→ Return JWT token + user details
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Store token in localStorage
                            └─ Redirect to dashboard
```

### Steps

1. User enters registration details (email, password, name, phone, address, state, district)
2. Frontend validates input
3. Frontend sends POST request to `/api/v1/auth/register`
4. API Gateway validates request and routes to User Service
5. User Service:
   - Validates email format
   - Checks if email already exists
   - Hashes password using BCrypt
   - Creates User entity
   - Stores in PostgreSQL
   - Generates JWT token (24-hour expiration)
6. Returns JWT token and user details
7. Frontend stores token in localStorage
8. Frontend redirects to dashboard

### Error Handling

- Invalid email format → 400 Bad Request
- Email already exists → 409 Conflict
- Weak password → 400 Bad Request
- Database error → 500 Internal Server Error

---

## Crop Recommendation Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Enter soil/weather data
    │  (latitude, longitude, temperature, humidity, rainfall, pH, N, P, K)
    │
    └─→ POST /api/v1/crops/recommendations
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            ├─ Extract user info
            │
            └─→ Crop Service (8093)
                │
                ├─ Check Redis cache
                │
                ├─ If not cached:
                │  │
                │  ├─→ Weather API
                │  │   └─ Fetch current weather
                │  │
                │  ├─→ Kaegro Soil API
                │  │   └─ Fetch soil data
                │  │
                │  └─→ AI Service (8001)
                │      │
                │      ├─ Load Crop Recommendation Model
                │      ├─ Prepare features
                │      ├─ Run prediction
                │      └─ Return recommendation
                │
                ├─ Cache result in Redis (1 hour TTL)
                ├─ Store in PostgreSQL
                │
                └─→ Return recommendation
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display recommendation
                            └─ Show confidence score
                            └─ Show alternative crops
```

### Steps

1. User enters soil and weather parameters
2. Frontend validates input
3. Frontend sends POST request with data
4. API Gateway validates JWT token and routes to Crop Service
5. Crop Service:
   - Checks Redis cache for similar request
   - If not cached:
     - Fetches current weather from Weather API
     - Fetches soil data from Kaegro API
     - Calls AI Service with combined data
   - AI Service:
     - Loads Crop Recommendation Model
     - Prepares features (8 features)
     - Runs Random Forest prediction
     - Returns recommended crop with confidence
   - Caches result in Redis
   - Stores recommendation in PostgreSQL
6. Returns recommendation to frontend
7. Frontend displays:
   - Recommended crop
   - Confidence score
   - Alternative crops
   - Reasoning

### Error Handling

- Invalid coordinates → 400 Bad Request
- Weather API timeout → 502 Bad Gateway
- ML Service error → 503 Service Unavailable
- Database error → 500 Internal Server Error

---

## Market Price Search Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Select state, district, commodity, variety, grade
    │
    └─→ POST /api/v1/mandi/filter/search
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            │
            └─→ Mandi Service (8096)
                │
                ├─ Check Redis cache
                │
                ├─ If not cached:
                │  │
                │  ├─ Query PostgreSQL
                │  │  (state, district, commodity data)
                │  │
                │  ├─→ AGMARKNET API
                │  │   └─ Fetch commodity prices
                │  │
                │  └─→ data.gov.in API
                │      └─ Fetch mandi prices
                │
                ├─ Combine results
                ├─ Cache in Redis (24 hours TTL)
                ├─ Store in PostgreSQL
                │
                └─→ Return market prices
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display market prices
                            └─ Show price trends
                            └─ Show volume data
```

### Steps

1. User selects search criteria (state, district, commodity, variety, grade)
2. Frontend sends POST request with filters
3. API Gateway validates JWT token and routes to Mandi Service
4. Mandi Service:
   - Checks Redis cache for similar search
   - If not cached:
     - Queries PostgreSQL for reference data
     - Calls AGMARKNET API for commodity prices
     - Calls data.gov.in API for mandi prices
   - Combines results from multiple sources
   - Caches in Redis (24-hour TTL)
   - Stores in PostgreSQL
5. Returns market prices with:
   - Min price, max price, modal price
   - Volume traded
   - Date of data
   - Source (AGMARKNET or data.gov.in)
6. Frontend displays:
   - Market prices in table format
   - Price trends (if historical data available)
   - Volume data
   - Supplier information

### Error Handling

- Invalid filters → 400 Bad Request
- AGMARKNET API timeout → 502 Bad Gateway
- data.gov.in API error → 502 Bad Gateway
- No data found → 404 Not Found

---

## Weather Advisory Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Select state, district, crop
    │
    └─→ GET /api/v1/weather/agromet-advisory
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            │
            └─→ Weather Service (8100)
                │
                ├─ Check Redis cache
                │
                ├─ If not cached:
                │  │
                │  ├─→ IMD API
                │  │   └─ Fetch agromet advisory
                │  │
                │  └─→ Weather API
                │      └─ Fetch current weather
                │
                ├─ Generate advisory
                ├─ Cache in Redis (6 hours TTL)
                ├─ Store in PostgreSQL
                │
                └─→ Return advisory
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display advisory
                            └─ Show recommendations
                            └─ Show alerts
```

### Steps

1. User selects state, district, and crop
2. Frontend sends GET request
3. API Gateway validates JWT token and routes to Weather Service
4. Weather Service:
   - Checks Redis cache
   - If not cached:
     - Calls IMD API for agromet advisory
     - Calls Weather API for current conditions
   - Generates advisory based on:
     - Current weather
     - Forecast
     - Crop requirements
   - Caches in Redis (6-hour TTL)
   - Stores in PostgreSQL
5. Returns advisory with:
   - Advisory text
   - Recommendations
   - Risk level (LOW, MEDIUM, HIGH)
   - Valid period
6. Frontend displays:
   - Advisory text
   - Recommendations (bullet points)
   - Risk level indicator
   - Weather alerts (if any)

### Error Handling

- Invalid state/district → 400 Bad Request
- IMD API timeout → 502 Bad Gateway
- No advisory available → 404 Not Found

---

## Disease Detection Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Upload crop image
    ├─ Select crop type
    │
    └─→ POST /api/ml/disease-detect
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            ├─ Validate file (size, type)
            │
            └─→ AI Service (8001)
                │
                ├─ Validate image
                ├─ Resize image
                │
                ├─→ AWS Lambda
                │   │
                │   ├─ Load disease detection model
                │   ├─ Preprocess image
                │   ├─ Run inference
                │   └─ Return disease classification
                │
                ├─ Get treatment info
                ├─ Get preventive measures
                │
                └─→ Return disease info
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display disease name
                            └─ Show confidence
                            └─ Show treatment
                            └─ Show prevention
```

### Steps

1. User uploads crop image
2. User selects crop type
3. Frontend validates image (size < 10MB, format: JPG/PNG)
4. Frontend sends POST request with image
5. API Gateway validates JWT token and routes to AI Service
6. AI Service:
   - Validates image format and size
   - Resizes image to model input size
   - Calls AWS Lambda function
   - Lambda:
     - Loads disease detection model
     - Preprocesses image
     - Runs deep learning inference
     - Returns disease classification with confidence
   - Gets treatment information
   - Gets preventive measures
7. Returns disease information:
   - Disease name
   - Confidence score
   - Severity level
   - Treatment recommendations
   - Preventive measures
8. Frontend displays:
   - Disease name and description
   - Confidence percentage
   - Treatment steps
   - Preventive measures
   - Recommendation to contact expert if needed

### Error Handling

- Invalid image format → 400 Bad Request
- Image too large → 413 Payload Too Large
- AWS Lambda error → 502 Bad Gateway
- Disease not recognized → 404 Not Found

---

## Voice Assistant Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Ask question in local language (e.g., Hindi)
    │
    └─→ POST /api/ml/ask-question
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            │
            └─→ AI Service (8001)
                │
                ├─ Detect language
                │
                ├─→ Bhashini API
                │   └─ Translate question to English
                │
                ├─→ AWS Bedrock (Claude 3 Sonnet)
                │   │
                │   ├─ Prepare prompt with context
                │   ├─ Call LLM
                │   └─ Get answer
                │
                ├─→ Bhashini API
                │   └─ Translate answer back to original language
                │
                └─→ Return answer
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display answer
                            └─ Show sources
                            └─ Show confidence
```

### Steps

1. User asks question in local language (Hindi, Marathi, Tamil, etc.)
2. Frontend sends POST request with question and language
3. API Gateway validates JWT token and routes to AI Service
4. AI Service:
   - Detects language (if not provided)
   - Calls Bhashini API to translate question to English
   - Prepares prompt with context:
     - User's state, district
     - User's crops
     - Current weather
     - Recent recommendations
   - Calls AWS Bedrock with Claude 3 Sonnet model
   - Claude generates answer
   - Calls Bhashini API to translate answer back to original language
5. Returns answer with:
   - Answer text
   - Sources (IMD, ICAR, etc.)
   - Confidence score
6. Frontend displays:
   - Answer in user's language
   - Sources
   - Option to ask follow-up question

### Supported Languages

- Hindi (hi)
- Marathi (mr)
- Tamil (ta)
- Telugu (te)
- Kannada (kn)
- Malayalam (ml)
- Gujarati (gu)
- Punjabi (pa)
- Bengali (bn)
- Odia (or)

### Error Handling

- Unsupported language → 400 Bad Request
- AWS Bedrock error → 502 Bad Gateway
- Bhashini API error → 502 Bad Gateway
- Invalid question → 400 Bad Request

---

## Yield Calculation Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Enter crop, area, soil type, weather data
    │
    └─→ POST /api/v1/crops/yield/calculate
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            │
            └─→ Yield Service (8094)
                │
                ├─ Query PostgreSQL
                │  (commodity yield parameters)
                │
                ├─ Calculate yield
                │  (base yield × factors)
                │
                ├─ Get market price
                │  (from Mandi Service)
                │
                ├─ Calculate revenue
                │  (yield × price)
                │
                ├─ Calculate costs
                │  (production + labor)
                │
                ├─ Calculate profit
                │  (revenue - costs)
                │
                └─→ Return yield & revenue
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display yield estimate
                            └─ Show revenue
                            └─ Show profit
                            └─ Show break-even price
```

### Steps

1. User enters:
   - Crop type
   - Area (hectares)
   - Soil type
   - Soil pH
   - Temperature
   - Humidity
   - Rainfall
   - Fertilizer used (N, P, K)
   - Irrigation method

2. Frontend sends POST request
3. API Gateway validates JWT token and routes to Yield Service
4. Yield Service:
   - Queries PostgreSQL for commodity yield parameters
   - Calculates yield using formula:
     - Base Yield × Soil Factor × Weather Factor × Nutrient Factor
   - Gets current market price from Mandi Service
   - Calculates revenue:
     - Yield (tons) × Market Price (per ton)
   - Calculates costs:
     - Production cost (seeds, fertilizer, pesticides)
     - Labor cost
     - Total cost
   - Calculates profit:
     - Revenue - Total Cost
   - Calculates break-even price:
     - Total Cost / Yield
5. Returns:
   - Estimated yield (tons/hectare)
   - Confidence score
   - Revenue estimate
   - Profit estimate
   - Break-even price
   - Recommendations
6. Frontend displays:
   - Yield estimate with confidence
   - Revenue projection
   - Profit projection
   - Break-even price
   - Comparison with historical data

### Error Handling

- Invalid crop → 400 Bad Request
- Invalid area → 400 Bad Request
- Commodity not found → 404 Not Found
- Market price unavailable → 502 Bad Gateway

---

## Government Scheme Search Flow

### Sequence Diagram

```
User (Frontend)
    │
    ├─ Enter state, commodity, farmer type, land holding
    │
    └─→ POST /api/v1/schemes/search
        │
        └─→ API Gateway (8080)
            │
            ├─ Validate JWT token
            │
            └─→ Scheme Service (8097)
                │
                ├─ Check Redis cache
                │
                ├─ If not cached:
                │  │
                │  └─ Query PostgreSQL
                │     (schemes matching criteria)
                │
                ├─ Filter by eligibility
                │  (farmer type, land holding, income)
                │
                ├─ Cache in Redis (24 hours TTL)
                │
                └─→ Return eligible schemes
                    │
                    └─→ API Gateway
                        │
                        └─→ Frontend
                            │
                            └─ Display schemes
                            └─ Show eligibility
                            └─ Show benefits
                            └─ Show application link
```

### Steps

1. User enters search criteria:
   - State
   - Commodity
   - Farmer type (MARGINAL, SMALL, MEDIUM, LARGE)
   - Land holding (hectares)
   - Annual income

2. Frontend sends POST request
3. API Gateway validates JWT token and routes to Scheme Service
4. Scheme Service:
   - Checks Redis cache for similar search
   - If not cached:
     - Queries PostgreSQL for schemes
     - Filters by state and commodity
   - Checks eligibility for each scheme:
     - Farmer type match
     - Land holding within range
     - Income within limit
   - Caches results in Redis (24-hour TTL)
5. Returns eligible schemes with:
   - Scheme name
   - Description
   - Benefits (amount, frequency)
   - Eligibility criteria
   - Application deadline
   - Application URL
   - Contact information
6. Frontend displays:
   - List of eligible schemes
   - Scheme details
   - Application link
   - Contact information

### Error Handling

- Invalid state → 400 Bad Request
- Invalid commodity → 400 Bad Request
- No schemes found → 404 Not Found
- Database error → 500 Internal Server Error
