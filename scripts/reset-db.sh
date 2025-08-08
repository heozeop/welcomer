#!/bin/bash
# Database reset script for Welcomer SNS Feed System development environment

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
DATABASE="all"
FORCE="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -d|--database)
            DATABASE="$2"
            shift 2
            ;;
        -f|--force)
            FORCE="true"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --env ENVIRONMENT    Set environment (dev, test, staging) [default: dev]"
            echo "  -d, --database DATABASE  Reset specific database (mysql, mongodb, redis, all) [default: all]"
            echo "  -f, --force             Skip confirmation prompts"
            echo "  -h, --help              Show this help message"
            echo ""
            echo "This script will reset databases by:"
            echo "  1. Stopping the database containers"
            echo "  2. Removing the data volumes"
            echo "  3. Restarting the containers (fresh data)"
            echo ""
            echo "Examples:"
            echo "  $0                      # Reset all databases"
            echo "  $0 -d mysql            # Reset only MySQL"
            echo "  $0 -d mongodb          # Reset only MongoDB"
            echo "  $0 -e test             # Reset in test environment"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_warning "This will reset database(s) in the Welcomer SNS Feed System!"
print_warning "Environment: $ENVIRONMENT"
print_warning "Database(s): $DATABASE"
print_error "ALL DATA WILL BE LOST!"

# Confirmation
if [ "$FORCE" != "true" ]; then
    echo ""
    read -p "Are you sure? (type 'yes' to confirm): " confirmation
    if [ "$confirmation" != "yes" ]; then
        print_info "Database reset cancelled."
        exit 0
    fi
fi

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

# Function to reset MySQL
reset_mysql() {
    print_info "Resetting MySQL database..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" stop mysql
    docker volume rm -f welcomer-mysql-data 2>/dev/null || true
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d mysql
    print_success "MySQL database reset completed!"
}

# Function to reset MongoDB
reset_mongodb() {
    print_info "Resetting MongoDB database..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" stop mongodb
    docker volume rm -f welcomer-mongodb-data 2>/dev/null || true
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d mongodb
    print_success "MongoDB database reset completed!"
}

# Function to reset Redis
reset_redis() {
    print_info "Resetting Redis cache..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" stop redis
    docker volume rm -f welcomer-redis-data 2>/dev/null || true
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d redis
    print_success "Redis cache reset completed!"
}

# Function to reset Kafka
reset_kafka() {
    print_info "Resetting Kafka and Zookeeper..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" stop kafka zookeeper
    docker volume rm -f welcomer-kafka-data 2>/dev/null || true
    docker volume rm -f welcomer-zookeeper-data 2>/dev/null || true
    docker volume rm -f welcomer-zookeeper-logs 2>/dev/null || true
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d zookeeper kafka
    print_success "Kafka and Zookeeper reset completed!"
}

# Function to reset Elasticsearch
reset_elasticsearch() {
    print_info "Resetting Elasticsearch..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" stop elasticsearch
    docker volume rm -f welcomer-elasticsearch-data 2>/dev/null || true
    $DOCKER_COMPOSE --env-file "$ENV_FILE" up -d elasticsearch
    print_success "Elasticsearch reset completed!"
}

print_info "Starting database reset process..."

# Reset based on selection
case $DATABASE in
    mysql)
        reset_mysql
        ;;
    mongodb)
        reset_mongodb
        ;;
    redis)
        reset_redis
        ;;
    kafka)
        reset_kafka
        ;;
    elasticsearch)
        reset_elasticsearch
        ;;
    all)
        reset_mysql
        reset_mongodb
        reset_redis
        reset_kafka
        reset_elasticsearch
        ;;
    *)
        print_error "Unknown database: $DATABASE"
        print_error "Supported databases: mysql, mongodb, redis, kafka, elasticsearch, all"
        exit 1
        ;;
esac

# Wait for services to be ready
print_info "Waiting for services to be ready..."
sleep 10

# Check health
print_info "Checking service health..."
for i in {1..30}; do
    if $DOCKER_COMPOSE --env-file "$ENV_FILE" ps | grep -q "healthy\|running"; then
        break
    fi
    if [ $i -eq 30 ]; then
        print_warning "Services may still be starting. Check logs if needed: ./scripts/logs.sh"
        break
    fi
    sleep 2
done

print_success "Database reset completed successfully!"
print_info ""
print_info "You can now:"
print_info "  - View logs: ./scripts/logs.sh"
print_info "  - Check health: ./scripts/start.sh"
print_info "  - Access services via the management UIs"