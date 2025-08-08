#!/bin/bash
# Stop script for Welcomer SNS Feed System development environment

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
REMOVE_VOLUMES="false"

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
        -v|--volumes)
            REMOVE_VOLUMES="true"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --env ENVIRONMENT    Set environment (dev, test, staging) [default: dev]"
            echo "  -s, --services SERVICES  Stop specific services [default: all]"
            echo "  -v, --volumes           Also remove volumes (WARNING: This will delete all data!)"
            echo "  -h, --help              Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                      # Stop all services"
            echo "  $0 -e test             # Stop with test environment"
            echo "  $0 -s \"mysql redis\"    # Stop only MySQL and Redis"
            echo "  $0 -v                  # Stop all services and remove volumes"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_info "Stopping Welcomer SNS Feed System..."
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

# Stop services
if [ "$SERVICES" = "all" ]; then
    print_info "Stopping all services..."
    $DOCKER_COMPOSE --env-file "$ENV_FILE" down
else
    print_info "Stopping services: $SERVICES"
    $DOCKER_COMPOSE --env-file "$ENV_FILE" stop $SERVICES
fi

# Remove volumes if requested
if [ "$REMOVE_VOLUMES" = "true" ]; then
    print_warning "Removing volumes - This will delete all data!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        $DOCKER_COMPOSE --env-file "$ENV_FILE" down -v
        
        # Remove named volumes
        print_info "Removing named volumes..."
        docker volume rm -f welcomer-mysql-data 2>/dev/null || true
        docker volume rm -f welcomer-mongodb-data 2>/dev/null || true
        docker volume rm -f welcomer-redis-data 2>/dev/null || true
        docker volume rm -f welcomer-zookeeper-data 2>/dev/null || true
        docker volume rm -f welcomer-zookeeper-logs 2>/dev/null || true
        docker volume rm -f welcomer-kafka-data 2>/dev/null || true
        docker volume rm -f welcomer-elasticsearch-data 2>/dev/null || true
        
        print_success "All volumes removed successfully!"
    else
        print_info "Volume removal cancelled."
    fi
fi

# Remove orphan containers
print_info "Removing orphan containers..."
$DOCKER_COMPOSE --env-file "$ENV_FILE" down --remove-orphans

# Show stopped services
RUNNING_SERVICES=$($DOCKER_COMPOSE --env-file "$ENV_FILE" ps -q | wc -l | tr -d ' ')
if [ "$RUNNING_SERVICES" -eq 0 ]; then
    print_success "All services stopped successfully!"
else
    print_warning "$RUNNING_SERVICES services are still running"
    $DOCKER_COMPOSE --env-file "$ENV_FILE" ps
fi

print_info "To start services again, run: ./scripts/start.sh"