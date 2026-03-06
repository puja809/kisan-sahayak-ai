# Admin Service

The Admin Service provides administrative functions including document management, analytics, audit logs, and system configuration for the Indian Farmer Assistance Application.

## 🚀 Overview

- **Port:** 8091
- **Technology Stack:** Java (Spring Boot), PostgreSQL, AWS S3
- **Primary Purpose:** Document management (training materials, advisories), system analytics, and audit logging.

## 🛠️ Key Features

- **Document Management:** Secure upload, storage (AWS S3), and retrieval of agricultural training materials and advisories.
- **Analytics Dashboard:** Real-time statistics on users, crops, and recommendation accuracy.
- **Audit Logs:** Comprehensive tracking of system actions and user activities for security and compliance.
- **System Configuration:** Centralized management of application settings.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/documents` | Upload document (S3 integration) |
| GET | `/api/v1/admin/analytics/dashboard` | Get overall system analytics |
| GET | `/api/v1/admin/audit-logs` | Retrieve system audit trails |

## ⚙️ Configuration

The service requires the following environment variables:
- `ADMIN_DB_URL`: PostgreSQL connection URL
- `AWS_S3_BUCKET_NAME`: S3 bucket for document storage
- `AWS_ACCESS_KEY_ID`: AWS credentials
- `AWS_SECRET_ACCESS_KEY`: AWS credentials

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Admin Service Documentation](../../documentations/services/MANDI_SCHEME_LOCATION_YIELD_SERVICES.md#admin-service)
