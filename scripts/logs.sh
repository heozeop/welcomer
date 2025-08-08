#!/bin/bash
# Logs script for Welcomer SNS Feed System development environment

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

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Parse command line arguments
ENVIRONMENT="dev"
SERVICE=""
FOLLOW="false"
TAIL_LINES="100"

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -s|--service)
            SERVICE="$2"
            shift 2
            ;;
        -f|--follow)
            FOLLOW="true"
            shift
            ;;
        -n|--tail)
            TAIL_LINES="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --env ENVIRONMENT    Set environment (dev, test, staging) [default: dev]"
            echo "  -s, --service SERVICE    Show logs for specific service"
            echo "  -f, --follow            Follow log output"
            echo "  -n, --tail LINES        Number of lines to show [default: 100]"
            echo "  -h, --help              Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                      # Show logs for all services"
            echo "  $0 -s app              # Show logs for app service only"
            echo "  $0 -f                  # Follow logs for all services"
            echo "  $0 -s mysql -f         # Follow logs for MySQL service"
            echo "  $0 -n 50               # Show last 50 lines"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_info "Viewing logs for Welcomer SNS Feed System..."
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

# Build logs command
LOGS_CMD="$DOCKER_COMPOSE --env-file $ENV_FILE logs"

# Add tail option
LOGS_CMD="$LOGS_CMD --tail=$TAIL_LINES"

# Add follow option
if [ "$FOLLOW" = "true" ]; then
    LOGS_CMD="$LOGS_CMD -f"
fi

# Add service filter
if [ -n "$SERVICE" ]; then
    # Check if service exists
    if ! $DOCKER_COMPOSE --env-file "$ENV_FILE" ps --services | grep -q "^$SERVICE$"; then
        print_error "Service '$SERVICE' not found!"
        echo "Available services:"
        $DOCKER_COMPOSE --env-file "$ENV_FILE" ps --services
        exit 1
    fi
    LOGS_CMD="$LOGS_CMD $SERVICE"
    print_info "Showing logs for service: $SERVICE"
else
    print_info "Showing logs for all services"
fi

if [ "$FOLLOW" = "true" ]; then
    print_info "Following logs... (Press Ctrl+C to stop)"
else
    print_info "Showing last $TAIL_LINES lines"
fi

echo ""

# Execute logs command
exec $LOGS_CMD