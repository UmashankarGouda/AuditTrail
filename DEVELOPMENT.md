# DeployGuard Development Guide

## Prerequisites

### Required
- **Java 17+** - Download from [java.com](https://www.java.com/)
- **Maven 3.6+** - Download from [maven.apache.org](https://maven.apache.org/download.cgi)
- **PostgreSQL 12+** - Download from [postgresql.org](https://www.postgresql.org/download/)
- **Git** - Download from [git-scm.com](https://git-scm.com/)

### Optional
- **Docker & Docker Compose** - For containerized deployment
- **Postman** - For API testing
- **IntelliJ IDEA** or **VS Code** - IDE

## Setup Instructions

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/deployguard.git
cd deployguard
```

### 2. Create PostgreSQL Database
```sql
-- Connect to PostgreSQL as admin user, then run:
CREATE DATABASE deployguard_db;
CREATE USER deployguard_user WITH PASSWORD 'DeployGuard123!';
GRANT ALL PRIVILEGES ON DATABASE deployguard_db TO deployguard_user;
```

### 3. Configure Application
Edit `src/main/resources/application.properties`:
```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/deployguard_db
spring.datasource.username=deployguard_user
spring.datasource.password=DeployGuard123!

# JWT Configuration
security.jwt.secret=YOUR_SECRET_KEY_HERE
security.jwt.expiration=86400000
```

### 4. Build Project
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Full build with package
mvn clean package
```

### 5. Run Application
```bash
# Using Maven
mvn spring-boot:run

# Or directly with JAR
java -jar target/deployguard-0.1.0-BETA.jar
```

Application starts at `http://localhost:8080/deployguard`

## API Testing

### 1. Access Swagger UI
```
http://localhost:8080/deployguard/swagger-ui.html
```

### 2. Sample cURL Requests

**Register User:**
```bash
curl -X POST http://localhost:8080/deployguard/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.dev",
    "email": "john@example.com",
    "password": "password123",
    "role": "DEVELOPER"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/deployguard/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.dev",
    "password": "password123"
  }'
```

**Create Deployment:**
```bash
curl -X POST http://localhost:8080/deployguard/api/deployments \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q1 Release",
    "environment": "PRODUCTION",
    "deployedBy": "john.dev",
    "notes": "Critical production release"
  }'
```

## Git Workflow

### Feature Branch Development
```bash
# Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "Add your feature description"
git push origin feature/your-feature-name

# Create Pull Request
# Then after review and merge to develop, create PR to main
```

### Branch Protection Rules
- `main` - Requires PR review, all tests pass
- `develop` - Requires PR review, all tests pass

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=DeploymentServiceTest
```

### Generate Coverage Report
```bash
mvn clean test jacoco:report
# Report generated at: target/site/jacoco/index.html
```

### Expected Coverage
- **Target**: 80%+ overall
- **Services**: 85%+
- **Controllers**: 70%+

## Code Style & Standards

### Naming Conventions
- Classes: `PascalCase` (e.g., `DeploymentService`)
- Methods: `camelCase` (e.g., `getDeploymentById`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_BATCH_SIZE`)
- Variables: `camelCase` (e.g., `deploymentStatus`)

### Best Practices
- Use `@Service` for business logic
- Use `@Repository` for data access
- Use `@RestController` for API endpoints
- Add `@Transactional` for multi-step operations
- Use DTOs for API responses
- Add comprehensive Javadoc comments
- Write meaningful commit messages

## Database Migrations

### Manual Schema Update
```bash
# Spring JPA will auto-update schema based on entities
# Set in application.properties:
spring.jpa.hibernate.ddl-auto=update
```

## Deployment

### Local Deployment
```bash
mvn clean package
java -jar target/deployguard-0.1.0-BETA.jar
```

### Docker Deployment
```bash
# Build image
docker build -t deployguard:0.1.0-BETA .

# Run with docker-compose
docker-compose up -d

# Check logs
docker-compose logs -f deployguard
```

## Troubleshooting

### Issue: "Connection refused" on Database
**Solution**: Ensure PostgreSQL is running and credentials are correct

### Issue: "mvn: command not found"
**Solution**: Add Maven to PATH or use full path to mvn executable

### Issue: Port 8080 Already in Use
**Solution**: 
```bash
# Change port in application.properties:
server.port=8081
# Or kill process using port 8080:
lsof -i :8080 | awk '{print $2}' | xargs kill -9
```

### Issue: JWT Token Expired
**Solution**: Login again to get new token or increase expiration in properties

## Performance Optimization

### Batch Processing (Spring Batch)
- Chunk size: 500 records/chunk (configurable)
- Skip faulty records automatically
- Transaction per chunk for consistency

### Database Optimization
- Add indexes on frequently queried columns
- Use pagination for large result sets
- Enable query caching

### API Response
- Use pagination for list endpoints
- Compress responses with gzip
- Cache static content

## Security Considerations

### Important
- **Never commit secrets** to version control
- Use environment variables for sensitive data
- Regularly update dependencies: `mvn dependency:resolve`
- Enable HTTPS in production
- Implement rate limiting for APIs

### JWT Token
- Default expiration: 24 hours
- Should be stored securely on client
- Use HTTPS for token transmission

## Useful Commands

```bash
# Check Java version
java -version

# Check Maven version
mvn -v

# Download dependencies
mvn dependency:resolve

# Generate API docs
mvn springdoc-openapi-maven-plugin:generate

# Run specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Build without tests
mvn clean package -DskipTests

# Update all Maven plugins
mvn plugins:update
```

## IDE Setup

### IntelliJ IDEA
1. File → Open → Select project root
2. Right-click pom.xml → Maven → Generate Sources and Update Folders
3. Set Java SDK: File → Project Structure → SDK → 17

### VS Code
1. Install "Extension Pack for Java"
2. Install "Spring Boot Extension Pack"
3. Open project folder
4. Run: Debug → Start Debugging

## CI/CD Pipeline

Automated testing and deployment via GitHub Actions:
- Test runs on: `git push` to any branch
- Deploy runs on: `git push` to `main` branch
- Notifications sent to team

See `.github/workflows/` for configuration.

## Support & Issues

- Check existing issues: [GitHub Issues](https://github.com/yourusername/deployguard/issues)
- Create new issue for bugs or features
- Follow issue template for consistency

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Batch](https://spring.io/projects/spring-batch)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT Introduction](https://jwt.io/introduction)
