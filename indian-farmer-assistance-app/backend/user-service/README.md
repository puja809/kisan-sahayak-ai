# User Service

The User Service handles authentication, profile management, and integration with the Government of India's AgriStack (UFSI) for the Indian Farmer Assistance Application.

## 🚀 Overview

- **Port:** 8099
- **Technology Stack:** Java (Spring Boot), PostgreSQL, JWT
- **Primary Purpose:** Secure user registration, authentication, and profile tracking for personalized services.

## 🛠️ Key Features

- **Authentication:** Secure login and registration using JWT-based tokens.
- **AgriStack Integration:** Seamless integration with Unified Farmer Service Interface (UFSI) for farmer data validation.
- **Profile Management:** Manage farmer name, contact details, location, and preferred languages.
- **Security:** Implements role-based access control (RBAC) and data localization in compliance with the DPDP Act 2023.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/register` | Register a new farmer/user |
| POST | `/api/v1/users/login` | Login and receive JWT token |
| GET | `/api/v1/users/profile` | Get logged-in user details |
| PUT | `/api/v1/users/profile` | Update user profile |

## ⚙️ Configuration

The service requires the following environment variables:
- `USER_DB_URL`: PostgreSQL connection URL
- `EUREKA_SERVER_URL`: URL to the discovery server

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [User Service Documentation](../../documentations/services/USER_SERVICE.md)
