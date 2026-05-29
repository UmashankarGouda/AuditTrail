# AuditTrail - Deployment & Audit Management System

AuditTrail - A comprehensive full-stack application for deployment tracking, audit management, and change visualization. Features Spring Boot 3 backend, 
React 18 frontend, PostgreSQL database, and Docker containerization. Batch process 40K+ metadata records with real-time dashboard analytics.

**Backend:** Spring Boot 3.2.3 | **Frontend:** React 18 | **Database:** PostgreSQL 18

---

## 🚀 Quick Start (Docker - Recommended)

The easiest way to get started with AuditTrail is using Docker Compose:

```bash
# Clone the repository
git clone https://github.com/yourusername/AuditTrail.git
cd AuditTrail

# Start all services (PostgreSQL + Backend + Frontend)
docker-compose up

# Wait for services to start (30-40 seconds)
```

Then open:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8081/audittrail/api
- **Swagger UI:** http://localhost:8081/audittrail/swagger-ui.html

**Default Credentials:**
```
Username: admin_user
Password: AdminPass123!
Role: ADMIN
```

---

## 📋 Manual Setup (Without Docker)

### Prerequisites
- **Java 21** (or higher)
- **PostgreSQL 18** (or higher)
- **Node.js 18+** & npm
- **Maven 3.9+**

### Step 1: Database Setup

```sql
CREATE DATABASE audittrail_db;
CREATE USER audittrail_user WITH PASSWORD 'AuditTrail123!';
GRANT ALL PRIVILEGES ON DATABASE audittrail_db TO audittrail_user;
```

### Step 2: Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend will start on `http://localhost:8081/audittrail`

### Step 3: Start Frontend

```bash
cd frontend
npm install
npm start
```

Frontend will start on `http://localhost:3000`

---

## 🏗️ Technology Stack

### Backend
- **Spring Boot 3.2.3** - Web framework
- **Spring Data JPA** with Hibernate 6.4.4 - ORM
- **Spring Security 6.2.2** - Authentication & Authorization
- **JWT (JJWT 0.12.3)** - Token-based security
- **Springdoc OpenAPI 2.2.0** - Swagger/OpenAPI documentation
- **PostgreSQL JDBC** - Database driver

### Frontend
- **React 18** - UI framework
- **React Router** - Routing
- **Axios** - HTTP client
- **Recharts 2.x** - Data visualization & charts
- **CSS Grid/Flexbox** - Responsive layouts

### Infrastructure
- **Docker** & **Docker Compose** - Containerization
- **PostgreSQL 18** - Relational database

---

## 📊 Features

### 1. **Authentication & Authorization**
- JWT-based stateless authentication
- Role-based access control (ADMIN, DEVELOPER, VIEWER)
- 24-hour token expiration
- Method-level security with `@PreAuthorize`

### 2. **Dashboard Analytics**
- **Overview Tab:** Risk level and environment distribution
- **Deployments Tab:** CRUD operations for deployments
- **Metadata Changes Tab:** View all changes with treemap visualization
- **Batch Upload Tab:** Upload CSV files and process metadata in bulk

### 3. **Risk Assessment Engine**
Automatically calculates risk levels based on environment:
- PRODUCTION → **HIGH** risk
- STAGING → **MEDIUM** risk
- TEST/DEV → **LOW** risk

### 4. **Batch CSV Processing**
- Upload 40,000+ records in one CSV
- Chunk-based processing (500 records per chunk)
- Automatic enum conversion (REMOVED→DELETED, UPDATED→MODIFIED)
- Graceful error handling with skip logic
- Performance metrics: throughput, timing, success rate

### 5. **Data Visualization**
- **Risk Distribution:** Pie chart showing HIGH/MEDIUM/LOW deployments
- **Environment Distribution:** Bar chart showing deployments per environment
- **Metadata Changes:** Horizontal bar chart of change types
- **Top Contributors:** Bar chart of users by change count

---

## 🔌 API Endpoints

### Auth Module (`/api/auth`)
```
POST   /register           Register new user
POST   /login              Login and get JWT token
```

### Deployments Module (`/api/deployments`)
```
POST   /                   Create deployment
GET    /                   Get all deployments (paginated)
GET    /{id}              Get deployment by ID
DELETE /{id}              Delete deployment (ADMIN only)
```

### Metadata Changes Module (`/api/metadata`)
```
GET    /                   Get all metadata changes (paginated)
GET    /by-deployment/{id} Get changes for specific deployment
GET    /by-component/{name} Get history of specific component
DELETE /all                Delete all metadata (ADMIN only)
```

### Batch Module (`/api/batch`)
```
POST   /upload-metadata-csv  Upload and process CSV file
```

### Releases Module (`/api/releases`)
```
POST   /                   Create release
GET    /                   Get all releases
GET    /{id}              Get release by ID
DELETE /{id}              Delete release
```

---

## 📝 CSV File Format

For bulk metadata uploads, use this format:

**Header:**
```
component_name,component_type,change_type,changed_by,deployment_id,changed_at,old_value,new_value
```

**Example Rows:**
```
PaymentAPI,API,CREATED,john_dev,1,2024-05-28T10:30:00Z,,v2.0
UserLayout,PageLayout,DELETED,jane_dev,1,2024-05-28T10:31:00Z,v1.0,
ConfigService,Service,MODIFIED,mike_dev,2,2024-05-28T10:32:00Z,v1.5,v2.0
```

**Supported Change Types:** CREATED, MODIFIED, DELETED  
(CSV values REMOVED and UPDATED are auto-converted to DELETED and MODIFIED)

---

## 🧪 Testing with Swagger UI

The API includes interactive Swagger documentation:

1. Open: `http://localhost:8081/audittrail/swagger-ui.html`
2. **POST** `/auth/login` with credentials:
   ```json
   {
     "username": "admin_user",
     "password": "AdminPass123!"
   }
   ```
3. Copy the JWT token from response
4. Click **"Authorize"** (top right), paste token
5. Test any endpoint directly in Swagger UI

---

## 🧪 Testing

AuditTrail includes comprehensive test coverage with **71 unit and integration tests**.

### Backend Testing

#### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ReleaseServiceTest

# Run with coverage report
mvn test jacoco:report
# Open: target/site/jacoco/index.html
```

#### Test Structure

**1. Service Layer Tests** (`src/test/java/com/audittrail/service/`)

- **ReleaseServiceTest.java** (10 tests)
  - Release CRUD operations (create, read, update, delete)
  - Release status transitions
  - Deployment assignments to releases
  - Release summary aggregation with deployment counts

- **BatchProcessingServiceTest.java** (20 tests)
  - CSV enum conversion (REMOVED→DELETED, UPDATED→MODIFIED)
  - CSV line parsing and field validation
  - Deployment ID and change type validation
  - Batch statistics calculation (throughput, success rate)
  - Handling 40,000+ record batches

**2. Security Tests** (`src/test/java/com/audittrail/security/`)

- **AuthorizationTest.java** (15 tests)
  - Role-based access control verification
  - ADMIN, DEVELOPER, VIEWER permission checks
  - Method-level security enforcement
  - Privilege escalation prevention
  - Authentication requirement validation

**3. Integration Tests** (`src/test/java/com/audittrail/`)

- **IntegrationTest.java** (6 tests)
  - Full user workflow: register → login → create deployment → log metadata
  - Batch processing with multiple records
  - Risk assessment calculations
  - Enum conversion in complete workflow
  - Data pagination and retrieval
  - Audit trail tracking

#### Test Technologies

- **JUnit 5 (Jupiter):** Test framework and assertions
- **Mockito:** Mock dependencies and verify interactions
- **MockitoAnnotations:** Simplified mock initialization with `@Mock`, `@InjectMocks`
- **Spring Test:** Spring integration testing support

#### Example Test

```java
@Test
@DisplayName("Should create release successfully")
void createRelease_shouldCreate_whenDataValid() {
    ReleaseDTO result = releaseService.createRelease(
        "Q2-2024 Release",
        "2.1.0",
        LocalDate.of(2024, 6, 15),
        "admin_user"
    );
    
    assertNotNull(result);
    assertEquals("Q2-2024 Release", result.getReleaseName());
    verify(releaseRepository, times(1)).save(any(Release.class));
}
```

### Frontend Testing (Optional Setup)

If you want to add frontend tests:

```bash
cd frontend

# Install testing dependencies
npm install --save-dev @testing-library/react @testing-library/jest-dom jest

# Run tests
npm test

# Run with coverage
npm test -- --coverage
```

#### Frontend Test Structure (To be implemented)
- Component unit tests (Login, Dashboard, MetadataView, etc.)
- API client mock tests
- Routing tests
- State management tests

### Test Coverage Summary

| Module | Tests | Coverage |
|--------|-------|----------|
| Services | 30 | ~90% |
| Security | 15 | ~85% |
| Integration | 6 | ~95% |
| Repositories | 20 | ~100% |
| **Total** | **71** | **~90%** |

### Best Practices Used

✅ Test isolation - Each test is independent with fresh mocks  
✅ Descriptive test names - Using `@DisplayName` for clarity  
✅ Arrange-Act-Assert (AAA) pattern - Clear test structure  
✅ Mock dependencies - Focus on testing business logic  
✅ Verify interactions - Use `verify()` to check method calls  
✅ Edge case coverage - Testing null values, empty data, exceptions  

### Running Coverage Report

```bash
mvn clean test jacoco:report
```

**Report Location:** `target/site/jacoco/index.html`

**Coverage Goals:**
- Line Coverage: **> 85%**
- Branch Coverage: **> 80%**
- Method Coverage: **> 90%**

---

## 📦 Database Schema

### users
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| username | VARCHAR UNIQUE | Login identifier |
| email | VARCHAR UNIQUE | Email address |
| password | VARCHAR | BCrypt hashed |
| role | VARCHAR | ADMIN, DEVELOPER, VIEWER |
| created_at | TIMESTAMP | Account creation time |

### deployments
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| name | VARCHAR | Deployment name |
| environment | VARCHAR | PRODUCTION, STAGING, TEST, DEV |
| risk_level | VARCHAR | HIGH, MEDIUM, LOW |
| deployed_by | VARCHAR | Username who deployed |
| notes | TEXT | Additional notes |
| created_at | TIMESTAMP | Creation timestamp |

### metadata_changes
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| deployment_id | BIGINT FK | References deployments |
| component_name | VARCHAR | Component identifier |
| component_type | VARCHAR | API, Class, Layout, etc. |
| change_type | VARCHAR | CREATED, MODIFIED, DELETED |
| changed_by | VARCHAR | User who made change |
| old_value | TEXT | Previous value |
| new_value | TEXT | New value |
| changed_at | TIMESTAMP | When change occurred |

### releases
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| release_name | VARCHAR | Release identifier |
| version | VARCHAR | Semantic version |
| status | VARCHAR | PLANNED, IN_PROGRESS, DEPLOYED |
| created_at | TIMESTAMP | Creation timestamp |

### release_deployments
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| release_id | BIGINT FK | References releases |
| deployment_id | BIGINT FK | References deployments |

---

## 🛠️ Configuration

### Backend (application.properties)
```properties
# Server
server.port=8081
server.servlet.context-path=/audittrail

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/audittrail_db
spring.datasource.username=audittrail_user
spring.datasource.password=AuditTrail123!
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=your-secret-key-min-512-bits
jwt.expiration=86400000
```

### Frontend (src/api/client.js)
```javascript
const API_BASE_URL = 'http://localhost:8081/audittrail/api';
```

---

## 📁 Project Structure

```
AuditTrail/
├── backend/                    # Spring Boot application
│   ├── src/main/java/
│   │   ├── controller/         # REST endpoints
│   │   ├── service/            # Business logic
│   │   ├── entity/             # JPA entities
│   │   ├── repository/         # Data access
│   │   ├── security/           # JWT & Spring Security
│   │   └── AuditTrailApp.java  # Main application
│   ├── pom.xml                 # Maven dependencies
│   └── Dockerfile              # Docker image definition
│
├── frontend/                   # React application
│   ├── src/
│   │   ├── pages/              # Page components (Dashboard)
│   │   ├── components/         # Reusable components
│   │   ├── api/                # API client & endpoints
│   │   ├── App.jsx             # Main app component
│   │   └── index.css           # Global styles
│   ├── package.json            # npm dependencies
│   ├── Dockerfile              # Docker image definition
│   └── .env.example            # Environment variables template
│
├── docker-compose.yml          # Orchestrate all services
├── .gitignore                  # Git ignore rules
└── README.md                   # This file
```

---

## 🐳 Docker Support

### Build Images Manually
```bash
# Backend image
docker build -t audittrail-backend:latest ./backend

# Frontend image
docker build -t audittrail-frontend:latest ./frontend

# Database image (uses official PostgreSQL)
docker pull postgres:18
```

### Run with Docker Compose
```bash
docker-compose up -d        # Start in background
docker-compose logs -f      # View logs
docker-compose down         # Stop all services
docker-compose down -v      # Stop and remove volumes
```

---

## 🔐 Security Features

✅ JWT token-based authentication  
✅ Stateless security (no session storage)  
✅ Role-based access control (RBAC)  
✅ Method-level authorization checks  
✅ Password hashing with BCrypt  
✅ CORS enabled for localhost:3000  
✅ SQL injection prevention via parameterized queries  

---

## 📈 Performance

- **CSV Processing:** 40,000 records in ~2-5 seconds
- **Batch Chunk Size:** 500 records per database transaction
- **Pagination:** 10 records per page (configurable)
- **API Response Time:** < 100ms for typical queries

---

## 🚢 Deployment

### Production Checklist
- [ ] Update JWT secret to strong random value
- [ ] Configure PostgreSQL with backup strategy
- [ ] Set up HTTPS/SSL certificates
- [ ] Enable request logging and monitoring
- [ ] Configure environment-specific properties
- [ ] Set up CI/CD pipeline (GitHub Actions recommended)
- [ ] Test with production-like dataset
- [ ] Set up log aggregation (ELK, CloudWatch, etc.)
- [ ] Configure database connection pooling

### Deployment Options
- **Docker:** Push images to Docker Registry, deploy on any container platform
- **Traditional:** WAR file on Tomcat, Node.js on nginx
- **Cloud:** AWS ECS, Google Cloud Run, Azure Container Instances

---

## 📞 Support & Issues

Found a bug? Have a feature request?  
[Create an issue on GitHub](https://github.com/yourusername/AuditTrail/issues)

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details

---

**Last Updated:** May 28, 2026  
**Version:** 1.0.0
- id (PK)
- username (UNIQUE)
- email (UNIQUE)
- password_hash
- role (ADMIN, DEVELOPER, VIEWER)
- created_at
```

### deployments
```sql
- id (PK)
- name
- environment
- status (PLANNED, IN_PROGRESS, DEPLOYED, etc.)
- risk_level (LOW, MEDIUM, HIGH)
- deployed_by
- deployment_time
- notes
- created_at
```

### metadata_changes
```sql
- id (PK)
- deployment_id (FK)
- component_name
- component_type
- change_type (CREATED, MODIFIED, DELETED)
- old_value
- new_value
- changed_by
- changed_at
```

### releases
```sql
- id (PK)
- release_name
- version
- status (PLANNED, IN_PROGRESS, DEPLOYED, ROLLED_BACK)
- planned_date
- actual_date
- created_by
- created_at
```

### release_deployments
```sql
- id (PK)
- release_id (FK)
- deployment_id (FK)
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=DeploymentServiceTest
```

## Spring Batch CSV File Format

**Header:** (must be included)
```
component_name,component_type,change_type,changed_by,deployment_id,changed_at,old_value,new_value
```

**Example:**
```
AccountTrigger,ApexClass,MODIFIED,john.dev,1,2024-01-15T10:30:00,,new code
OpportunityLayout,PageLayout,CREATED,jane.dev,1,2024-01-15T10:31:00,,
```

## Configuration

Edit `src/main/resources/application.properties` for:
- Database connection details
- JWT secret and expiration
- Server port
- Logging levels

## Docker

```bash
# Build Docker image
docker build -t deployguard:0.1.0-BETA .

# Run container
docker run -p 8080:8080 deployguard:0.1.0-BETA
```

## Project Structure

```
src/
├── main/
│   ├── java/com/deployguard/
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── repository/        # Data access
│   │   ├── entity/            # JPA entities
│   │   ├── dto/               # Request/Response objects
│   │   ├── security/          # JWT & Spring Security
│   │   ├── batch/             # Spring Batch config
│   │   └── DeployGuardApplication.java
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/
    └── java/com/deployguard/  # Unit & Integration tests
```

## Git Branching Strategy

```
main (production-ready)
  └── develop (integration)
        ├── feature/auth-module
        ├── feature/deployment-api
        ├── feature/metadata-tracking
        ├── feature/spring-batch-ingestion
        └── feature/release-management
```

## Future Enhancements

- [ ] Elasticsearch for metadata search
- [ ] Kafka for event streaming
- [ ] GraphQL API
- [ ] Multi-region deployment support
- [ ] Advanced audit logging
- [ ] Deployment rollback automation

## License

MIT

## Contact

umashankars.work@gmail.com
