# Welcomer SNS Feed System

A comprehensive Social Networking Service (SNS) feed system built with Spring Boot 3.5.4, Kotlin, and a modern microservices architecture. This system provides content ingestion, processing, and feed generation capabilities with real-time event streaming and advanced analytics.

## 🏗️ Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web App]
        MOBILE[Mobile App]
        API[External APIs]
    end
    
    subgraph "Application Layer"
        APP[Spring Boot App<br/>Content Ingestion Service]
        HEALTH[Health Checks]
    end
    
    subgraph "Data Layer"
        MYSQL[(MySQL 8.0<br/>Primary Database)]
        MONGO[(MongoDB 7.0<br/>Document Store)]
        REDIS[(Redis 7<br/>Cache)]
    end
    
    subgraph "Streaming Layer"
        KAFKA[Apache Kafka<br/>Event Streaming]
        ZK[Zookeeper<br/>Coordination)]
    end
    
    subgraph "Search Layer"
        ES[Elasticsearch 8.15<br/>Search & Analytics]
    end
    
    subgraph "Management Layer"
        PMA[phpMyAdmin]
        ME[Mongo Express]
        RC[Redis Commander]
        KUI[Kafka UI]
    end
    
    WEB --> APP
    MOBILE --> APP
    API --> APP
    
    APP --> MYSQL
    APP --> MONGO
    APP --> REDIS
    APP --> KAFKA
    APP --> ES
    
    KAFKA --> ZK
    
    PMA --> MYSQL
    ME --> MONGO
    RC --> REDIS
    KUI --> KAFKA
```

## 🚀 Quick Start

### Prerequisites

- **Docker** (≥ 20.10.0) - [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose** (≥ 2.0.0) - Included with Docker Desktop
- **Git** - For cloning the repository
- At least **4GB RAM** available for containers
- **10GB disk space** for images and volumes

### 1. Clone and Setup

```bash
# Clone the repository
git clone <repository-url>
cd welcome

# Verify Docker is running
docker --version
docker-compose --version
```

### 2. Start the Development Environment

**Option A: Using Makefile (Recommended)**
```bash
# Quick start for new developers (cleans, builds, starts, opens UIs)
make quick-start

# Or start all services
make start

# Or start with fresh build
make build
```

**Option B: Using Scripts Directly**
```bash
# Start all services
./scripts/start.sh

# Or start with specific options
./scripts/start.sh --env dev --build
```

### 3. Verify Installation

After starting, check that services are running:

**Using Makefile:**
```bash
# Check service status and health
make health

# View logs
make logs

# Open all management UIs in browser
make ui
```

**Using Scripts/Docker:**
```bash
# Check service status
docker-compose ps

# View logs
./scripts/logs.sh

# Check application health
curl http://localhost:8080/actuator/health
```

## 📋 Service Access Points

### Application Services

| Service | URL | Description |
|---------|-----|-------------|
| **Main Application** | http://localhost:8080 | Spring Boot REST API |
| **Health Check** | http://localhost:8080/actuator/health | Application health status |
| **Metrics** | http://localhost:8080/actuator/metrics | Application metrics |
| **API Documentation** | http://localhost:8080/swagger-ui.html | Interactive API docs |

### Database Management (Development)

| Service | URL | Credentials |
|---------|-----|-------------|
| **phpMyAdmin** | http://localhost:8082 | `welcomer` / `devpassword` |
| **Mongo Express** | http://localhost:8083 | `admin` / `devpassword` |
| **Redis Commander** | http://localhost:8081 | Password: `devpassword` |
| **Kafka UI** | http://localhost:8090 | No authentication |

### Direct Database Connections

| Database | Host:Port | Credentials |
|----------|-----------|-------------|
| **MySQL** | localhost:3306 | `welcomer` / `devpassword` |
| **MongoDB** | localhost:27017 | `admin` / `devpassword` |
| **Redis** | localhost:6379 | Password: `devpassword` |
| **Kafka** | localhost:9092 | No authentication |
| **Elasticsearch** | localhost:9200 | `elastic` / `devpassword` |

## 🛠️ Development Commands

### Quick Reference (Makefile)

```bash
# Essential commands
make help                    # Show all available commands
make start                   # Start all services
make stop                    # Stop all services
make restart                 # Restart all services
make logs                    # View logs
make health                  # Check service health
make ui                      # Open management UIs

# Development workflows
make quick-start            # New developer setup (clean, build, start, open UIs)
make fresh                  # Clean slate (clean, build, health check)
make dev                    # Start development environment
make test-env               # Start test environment

# Database operations
make reset-db               # Reset all databases
make reset-mysql            # Reset only MySQL
make mysql-cli              # Connect to MySQL CLI
make mongo-cli              # Connect to MongoDB CLI
make redis-cli              # Connect to Redis CLI

# Maintenance
make clean                  # Clean up everything
make update                 # Pull latest images
make db-backup              # Create database backups
```

### Script Commands (Alternative)

```bash
# Start/stop services
./scripts/start.sh
./scripts/stop.sh
./scripts/logs.sh
./scripts/reset-db.sh
./scripts/clean.sh

# Environment management
./scripts/start.sh -e test
./scripts/start.sh -s "mysql redis app"
```

### Gradle Tasks

```bash
# Application development
make gradle-build           # Build project
make gradle-test            # Run tests  
make gradle-clean           # Clean build
make app-run                # Run app locally (not Docker)

# Or use Gradle directly
./gradlew bootRun
./gradlew test
./gradlew build
```

## 🔧 Configuration

### Environment Variables

The system uses `.env` files for configuration:

- `.env` - Development environment (default)
- `.env.test` - Testing environment
- `.env.staging` - Staging environment

Key configuration options:

```bash
# Database Configuration
MYSQL_DATABASE=welcomer_db
MYSQL_USER=welcomer
MYSQL_PASSWORD=devpassword

# Application Configuration  
APP_PORT=8080
SPRING_PROFILES_ACTIVE=dev
DEBUG_PORT=5005

# Service Versions
KAFKA_VERSION=7.4
ELASTIC_VERSION=8.15.0
```

### Custom Configuration

The single `docker-compose.yml` file includes both core services and development management tools. For production deployments, you can:

1. **Use environment-specific files**: Create `docker-compose.prod.yml` with production overrides
2. **Override via environment variables**: All settings are configurable through `.env` files
3. **Exclude development services**: Use `docker-compose up` with specific service names

Example for production override file:
```yaml
# docker-compose.prod.yml
services:
  app:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - LOGGING_LEVEL_ROOT=WARN
  # Remove development tools by not including them
```

## 🧪 Testing

### Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Test with specific environment
./scripts/start.sh -e test
./gradlew test --tests="*IntegrationTest"
```

### Test Data

The system includes initialization scripts that create test data:

- **MySQL**: See `init-scripts/mysql/01-init.sql`
- **MongoDB**: See `init-scripts/mongodb/01-init.js`

## 📊 Monitoring and Observability

### Health Checks

All services include health checks:

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check all service health  
docker-compose ps
```

### Logs

```bash
# View all logs
./scripts/logs.sh

# Follow application logs
./scripts/logs.sh -s app -f

# View specific service logs
./scripts/logs.sh -s mysql -n 50
```

### Metrics

Application metrics are available at:
- http://localhost:8080/actuator/metrics
- http://localhost:8080/actuator/prometheus (Prometheus format)

## 🔒 Security Notes

### Development vs Production

⚠️ **Important**: The single `docker-compose.yml` file is configured for **development only** and includes:

- Default passwords and weak credentials
- Disabled security features (Elasticsearch security disabled)
- Exposed management interfaces (phpMyAdmin, Mongo Express, etc.)
- Debug ports and development tools enabled

**Never use this configuration in production!**

For production, create a separate `docker-compose.prod.yml` that excludes development services and enables security features.

### Production Checklist

Before deploying to production:

- [ ] Create separate `docker-compose.prod.yml` without development services
- [ ] Change all default passwords in production environment files
- [ ] Enable TLS/SSL for all services
- [ ] Enable Elasticsearch security (`xpack.security.enabled=true`)
- [ ] Configure proper authentication for all services
- [ ] Remove development management UIs (phpMyAdmin, Mongo Express, etc.)
- [ ] Disable debug ports and development features
- [ ] Configure proper firewall rules
- [ ] Set up monitoring and alerting
- [ ] Use production-grade resource limits

## 🐛 Troubleshooting

### Common Issues

**Services won't start:**
```bash
# Check Docker is running
docker info

# Check available resources
docker system df

# Clean up if needed
./scripts/clean.sh -f
```

**Database connection errors:**
```bash
# Check database health
docker-compose ps
./scripts/logs.sh -s mysql

# Reset databases if needed
./scripts/reset-db.sh -d mysql
```

**Port conflicts:**
```bash
# Check what's using the ports
lsof -i :8080
lsof -i :3306

# Stop conflicting services or change ports in .env file
```

**Out of disk space:**
```bash
# Clean up Docker
docker system prune -a
./scripts/clean.sh -a -f

# Check disk usage
docker system df
```

### Getting Help

1. Check the logs: `./scripts/logs.sh`
2. Verify service health: `docker-compose ps`
3. Review configuration in `.env` files
4. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for detailed solutions

## 🏗️ Architecture Details

### Technology Stack

- **Backend**: Spring Boot 3.5.4 with Kotlin 1.9.25
- **Database**: MySQL 8.0 (primary), MongoDB 7.0 (documents)
- **Cache**: Redis 7 (caching, sessions)
- **Messaging**: Apache Kafka 7.4 with Zookeeper
- **Search**: Elasticsearch 8.15.0
- **Build**: Gradle 8.14.3 with Java 21
- **Containerization**: Docker & Docker Compose

### Key Features

- **Content Ingestion**: Multi-format content processing and validation
- **Real-time Processing**: Event-driven architecture with Kafka
- **Search & Analytics**: Full-text search with Elasticsearch
- **Caching**: Multi-layer caching with Redis
- **Health Monitoring**: Comprehensive health checks and metrics
- **Development Tools**: Integrated database management UIs

### Database Schema

The system uses Flyway migrations located in `src/main/resources/db/migration/`:

- **V1**: User profiles and social graph
- **V2**: Content and media models  
- **V3**: Engagement tracking (likes, comments, shares)
- **V4**: Algorithm parameters and A/B testing
- **V5**: Feed composition and caching
- **V6**: Performance indexes and constraints

## 🤝 Contributing

### Development Workflow

**Using Makefile (Recommended):**
1. Start development environment: `make dev` or `make quick-start`
2. Make your changes
3. Run tests: `make gradle-test`
4. Check code quality: `make gradle-check`
5. Test with fresh containers: `make fresh`

**Using Scripts:**
1. Start development environment: `./scripts/start.sh`
2. Make your changes
3. Run tests: `./gradlew test`
4. Check code quality: `./gradlew check`
5. Test with containers: `./scripts/start.sh -b`

### Code Style

The project follows standard Kotlin/Spring Boot conventions:
- Use meaningful variable and function names
- Add comprehensive tests for new features
- Include proper error handling
- Document complex logic with comments

## 📝 License

[Add your license information here]

## 🚀 Deployment

For production deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

---

**Happy coding! 🎉**

For questions or issues, please check the troubleshooting guide or open an issue in the project repository.