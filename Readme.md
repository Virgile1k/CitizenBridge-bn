# CitizenBridge

## Overview
CitizenBridge is a Spring Boot application designed to bridge the gap between citizens and public services. This platform facilitates communication, collaboration, and information sharing between community members and government institutions.

## Tech Stack
- **Backend**: Spring Boot 3.2.5 with Java 21
- **Database**: PostgreSQL and MongoDB
- **Authentication**: OAuth2, JWT, and Microsoft Authentication
- **Documentation**: SpringDoc OpenAPI
- **Cloud Storage**: AWS S3
- **AI Integration**: Google Cloud AI Platform (Gemini)
- **Email Service**: Spring Mail
- **Websockets**: STOMP for real-time communication

## App Documentation 
[Summarized System Architecture.pdf](docs/Summarized%20System%20Architecture.pdf)
## Prerequisites
- Java 21
- PostgreSQL
- MongoDB
- Maven
- AWS Account (for S3 storage)
- Google Cloud Account (for AI features)

## Configuration
The application uses environment variables for configuration. These can be set in a `.env` file in the root directory.

### Essential Environment Variables
```properties
# Database
DATABASE_URL=jdbc:postgresql://localhost:5433/citizenbridge
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# MongoDB
# Uses default configuration: mongodb://localhost:27017/citizenbridge

# JWT
JWT_SECRET=your_secret_key
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email
MAIL_PASSWORD=your_email_password

# AWS
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=eu-north-1
AWS_S3_BUCKET_NAME=your_bucket_name

# Google AI
GEMINI_API_KEY=your_gemini_api_key

# Application URLs
CITIZENBRIDGE_BASE_URL=http://localhost:3000

Installation & Setup
Clone the Repository
git clone https://github.com/Virgile1k/CitizenBridge-bn.git
cd CitizenBridge-bn



