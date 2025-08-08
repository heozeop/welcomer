#!/bin/bash
# Clean script for Welcomer SNS Feed System development environment

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
CLEAN_ALL="false"
FORCE="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -a|--all)
            CLEAN_ALL="true"
            shift
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
            echo "  -a, --all               Clean everything including images"
            echo "  -f, --force             Skip confirmation prompts"
            echo "  -h, --help              Show this help message"
            echo ""
            echo "This script will:"
            echo "  1. Stop all running containers"
            echo "  2. Remove all containers"
            echo "  3. Remove all volumes (data will be lost!)"
            echo "  4. Remove all networks"
            echo "  5. With --all: Remove all images"
            echo ""
            echo "WARNING: This will delete all data and is irreversible!"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_warning "This will clean up the Welcomer SNS Feed System environment!"
print_warning "Environment: $ENVIRONMENT"

if [ "$CLEAN_ALL" = "true" ]; then
    print_warning "This will remove EVERYTHING including Docker images!"
else
    print_warning "This will remove containers, volumes, and networks!"
fi

print_error "ALL DATA WILL BE LOST!"

# Confirmation
if [ "$FORCE" != "true" ]; then
    echo ""
    read -p "Are you absolutely sure? (type 'yes' to confirm): " confirmation
    if [ "$confirmation" != "yes" ]; then
        print_info "Cleanup cancelled."
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

print_info "Starting cleanup process..."

# Stop and remove containers, volumes, and networks
print_info "Stopping and removing containers, volumes, and networks..."
$DOCKER_COMPOSE --env-file "$ENV_FILE" down -v --remove-orphans 2>/dev/null || true

# Remove named volumes explicitly
print_info "Removing named volumes..."
docker volume rm -f welcomer-mysql-data 2>/dev/null || true
docker volume rm -f welcomer-mongodb-data 2>/dev/null || true
docker volume rm -f welcomer-redis-data 2>/dev/null || true
docker volume rm -f welcomer-zookeeper-data 2>/dev/null || true
docker volume rm -f welcomer-zookeeper-logs 2>/dev/null || true
docker volume rm -f welcomer-kafka-data 2>/dev/null || true
docker volume rm -f welcomer-elasticsearch-data 2>/dev/null || true

# Remove network
print_info "Removing network..."
docker network rm welcomer-network 2>/dev/null || true

# Remove dangling containers
print_info "Removing dangling containers..."
DANGLING_CONTAINERS=$(docker ps -aq -f status=exited -f name=welcomer 2>/dev/null || true)
if [ -n "$DANGLING_CONTAINERS" ]; then
    docker rm $DANGLING_CONTAINERS 2>/dev/null || true
fi

# Clean up images if requested
if [ "$CLEAN_ALL" = "true" ]; then
    print_info "Removing images..."
    
    # Remove welcomer images
    WELCOMER_IMAGES=$(docker images -q --filter=reference="welcomer*" 2>/dev/null || true)
    if [ -n "$WELCOMER_IMAGES" ]; then
        docker rmi -f $WELCOMER_IMAGES 2>/dev/null || true
    fi
    
    # Remove dangling images
    print_info "Removing dangling images..."
    DANGLING_IMAGES=$(docker images -q -f dangling=true 2>/dev/null || true)
    if [ -n "$DANGLING_IMAGES" ]; then
        docker rmi $DANGLING_IMAGES 2>/dev/null || true
    fi
    
    # Prune system
    print_info "Pruning Docker system..."
    docker system prune -f 2>/dev/null || true
fi

# Clean up local logs
print_info "Cleaning up local logs..."
rm -rf logs/* 2>/dev/null || true

# Clean up gradle build directory
print_info "Cleaning up build artifacts..."
./gradlew clean 2>/dev/null || true

print_success "Cleanup completed successfully!"
print_info ""
print_info "What was cleaned:"
print_info "✓ All containers stopped and removed"
print_info "✓ All volumes removed (data deleted)"
print_info "✓ All networks removed"
print_info "✓ Local logs cleaned"
print_info "✓ Build artifacts cleaned"

if [ "$CLEAN_ALL" = "true" ]; then
    print_info "✓ All images removed"
    print_info "✓ Docker system pruned"
fi

print_info ""
print_info "To start fresh, run: ./scripts/start.sh"