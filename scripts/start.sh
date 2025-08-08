#!/bin/bash
# Start script for Welcomer SNS Feed System development environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Parse command line arguments
ENVIRONMENT="dev"
SERVICES="all"
BUILD_IMAGES="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -s|--services)
            SERVICES="$2"
            shift 2
            ;;
        -b|--build)
            BUILD_IMAGES="true"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --env ENVIRONMENT    Set environment (dev, test, staging) [default: dev]"
            echo "  -s, --services SERVICES  Start specific services [default: all]"
            echo "  -b, --build             Force rebuild of images"
            echo "  -h, --help              Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                      # Start all services in development mode"
            echo "  $0 -e test             # Start with test environment"
            echo "  $0 -s \"mysql redis\"    # Start only MySQL and Redis"
            echo "  $0 -b                  # Start with forced image rebuild"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_info "Starting Welcomer SNS Feed System..."
print_info "Environment: $ENVIRONMENT"

# Set environment file
ENV_FILE=".env"
if [ "$ENVIRONMENT" != "dev" ]; then
    ENV_FILE=".env.$ENVIRONMENT"
    if [ ! -f "$ENV_FILE" ]; then
        print_error "Environment file $ENV_FILE not found!"
        exit 1
    fi
fi

print_info "Using environment file: $ENV_FILE"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    if ! docker compose version > /dev/null 2>&1; then
        print_error "Docker Compose is not installed."
        exit 1
    fi
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

print_info "Using Docker Compose command: $DOCKER_COMPOSE"

# Create required directories
mkdir -p logs config/ssl

# Set permissions for logs directory
chmod 755 logs

# Build options
BUILD_OPTS=""
if [ "$BUILD_IMAGES" = "true" ]; then
    BUILD_OPTS="--build"
    print_info "Forcing image rebuild..."
fi

# Start services
if [ "$SERVICES" = "all" ]; then
    print_info "Starting all services..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d $BUILD_OPTS
else
    print_info "Starting services: $SERVICES"
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d $BUILD_OPTS $SERVICES
fi

# Wait a moment for services to start
sleep 5

# Check service health
print_info "Checking service health..."
HEALTHY_SERVICES=0
TOTAL_SERVICES=0

for service in $($DOCKER_COMPOSE --env-file "$ENV_FILE" ps --services); do
    TOTAL_SERVICES=$((TOTAL_SERVICES + 1))
    if $DOCKER_COMPOSE --env-file "$ENV_FILE" ps "$service" | grep -q "healthy\|running"; then
        HEALTHY_SERVICES=$((HEALTHY_SERVICES + 1))
        print_success "$service is running"
    else
        print_warning "$service is not healthy yet"
    fi
done

print_info "Services status: $HEALTHY_SERVICES/$TOTAL_SERVICES running"

# Show access URLs
print_success "Development environment started successfully!"
echo ""
print_info "Service Access URLs:"
echo "  🚀 Application:      http://localhost:8080"
echo "  ❤️  Health Check:     http://localhost:8080/actuator/health"
echo "  📊 Application Metrics: http://localhost:8080/actuator/metrics"
echo ""
print_info "Database Management (Development only):"
echo "  🐬 phpMyAdmin:       http://localhost:8082"
echo "  🍃 Mongo Express:    http://localhost:8083"
echo "  🔴 Redis Commander:  http://localhost:8081"
echo "  📋 Kafka UI:         http://localhost:8090"
echo ""
print_info "Direct Database Connections:"
echo "  MySQL:    localhost:3306"
echo "  MongoDB:  localhost:27017"
echo "  Redis:    localhost:6379"
echo "  Kafka:    localhost:9092"
echo "  Elasticsearch: localhost:9200"
echo ""
print_info "Useful commands:"
echo "  View logs:           ./scripts/logs.sh"
echo "  Stop services:       ./scripts/stop.sh"
echo "  Reset databases:     ./scripts/reset-db.sh"
echo "  Clean everything:    ./scripts/clean.sh"