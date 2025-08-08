# Makefile for Welcomer SNS Feed System
# Provides convenient commands for Docker operations and development workflow

.DEFAULT_GOAL := help
.PHONY: help start stop restart logs clean reset-db build test lint status ps shell

# Environment variables
ENV ?= dev
SERVICE ?= 
FOLLOW ?= false

# Colors for output
BLUE := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
NC := \033[0m

##@ Docker Operations

start: ## Start all services or specific services (make start SERVICE="mysql redis")
	@echo "$(BLUE)[INFO]$(NC) Starting Welcomer SNS services..."
	@./scripts/start.sh $(if $(SERVICE),-s "$(SERVICE)") -e $(ENV)

stop: ## Stop all services or specific services (make stop SERVICE="mysql redis")
	@echo "$(BLUE)[INFO]$(NC) Stopping Welcomer SNS services..."
	@./scripts/stop.sh $(if $(SERVICE),-s "$(SERVICE)") -e $(ENV)

restart: stop start ## Restart all services

build: ## Build and start services with fresh images
	@echo "$(BLUE)[INFO]$(NC) Building and starting services..."
	@./scripts/start.sh -b -e $(ENV)

logs: ## View logs for all services or specific service (make logs SERVICE=app FOLLOW=true)
	@echo "$(BLUE)[INFO]$(NC) Viewing logs..."
	@./scripts/logs.sh $(if $(SERVICE),-s $(SERVICE)) $(if $(filter true,$(FOLLOW)),-f) -e $(ENV)

ps: status ## Alias for status

status: ## Show status of all services
	@echo "$(BLUE)[INFO]$(NC) Service status:"
	@docker-compose ps

##@ Database Operations

reset-db: ## Reset all databases (WARNING: destroys all data!)
	@echo "$(RED)[WARNING]$(NC) This will reset all databases!"
	@./scripts/reset-db.sh -e $(ENV)

reset-mysql: ## Reset only MySQL database
	@echo "$(YELLOW)[INFO]$(NC) Resetting MySQL database..."
	@./scripts/reset-db.sh -d mysql -e $(ENV)

reset-mongodb: ## Reset only MongoDB database
	@echo "$(YELLOW)[INFO]$(NC) Resetting MongoDB database..."
	@./scripts/reset-db.sh -d mongodb -e $(ENV)

reset-redis: ## Reset only Redis cache
	@echo "$(YELLOW)[INFO]$(NC) Resetting Redis cache..."
	@./scripts/reset-db.sh -d redis -e $(ENV)

##@ Development

dev: ## Start development environment with all services
	@echo "$(GREEN)[DEV]$(NC) Starting development environment..."
	@$(MAKE) start ENV=dev

test-env: ## Start test environment
	@echo "$(YELLOW)[TEST]$(NC) Starting test environment..."
	@$(MAKE) start ENV=test

staging-env: ## Start staging environment  
	@echo "$(BLUE)[STAGING]$(NC) Starting staging environment..."
	@$(MAKE) start ENV=staging

shell: ## Open shell in app container
	@echo "$(BLUE)[INFO]$(NC) Opening shell in app container..."
	@docker-compose exec app /bin/bash

##@ Application

app-logs: ## View application logs with follow
	@$(MAKE) logs SERVICE=app FOLLOW=true

app-build: ## Build application Docker image
	@echo "$(BLUE)[INFO]$(NC) Building application image..."
	@./gradlew bootBuildImage

app-test: ## Run application tests
	@echo "$(BLUE)[INFO]$(NC) Running application tests..."
	@./gradlew test

app-run: ## Run application locally (not in Docker)
	@echo "$(BLUE)[INFO]$(NC) Running application locally..."
	@./gradlew bootRun

##@ Database Access

mysql-cli: ## Connect to MySQL CLI
	@echo "$(BLUE)[INFO]$(NC) Connecting to MySQL..."
	@docker-compose exec mysql mysql -u$${MYSQL_USER:-welcomer} -p$${MYSQL_PASSWORD:-devpassword} $${MYSQL_DATABASE:-welcomer_db}

mongo-cli: ## Connect to MongoDB CLI
	@echo "$(BLUE)[INFO]$(NC) Connecting to MongoDB..."
	@docker-compose exec mongodb mongosh -u $${MONGO_INITDB_ROOT_USERNAME:-admin} -p $${MONGO_INITDB_ROOT_PASSWORD:-devpassword}

redis-cli: ## Connect to Redis CLI
	@echo "$(BLUE)[INFO]$(NC) Connecting to Redis..."
	@docker-compose exec redis redis-cli -a $${REDIS_PASSWORD:-devpassword}

##@ Management UIs

ui: ## Open all management UIs in browser
	@echo "$(GREEN)[INFO]$(NC) Opening management UIs..."
	@echo "$(BLUE)Main Application:$(NC) http://localhost:8080"
	@echo "$(BLUE)phpMyAdmin:$(NC) http://localhost:8082"
	@echo "$(BLUE)Mongo Express:$(NC) http://localhost:8083"
	@echo "$(BLUE)Redis Commander:$(NC) http://localhost:8081"
	@echo "$(BLUE)Kafka UI:$(NC) http://localhost:8090"
	@command -v open >/dev/null && (open http://localhost:8080 & open http://localhost:8082 & open http://localhost:8083 & open http://localhost:8081 & open http://localhost:8090) || echo "$(YELLOW)Open the URLs manually$(NC)"

phpmyadmin: ## Open phpMyAdmin in browser
	@echo "$(BLUE)[INFO]$(NC) Opening phpMyAdmin..."
	@command -v open >/dev/null && open http://localhost:8082 || echo "$(YELLOW)Open http://localhost:8082 manually$(NC)"

mongo-express: ## Open Mongo Express in browser
	@echo "$(BLUE)[INFO]$(NC) Opening Mongo Express..."
	@command -v open >/dev/null && open http://localhost:8083 || echo "$(YELLOW)Open http://localhost:8083 manually$(NC)"

redis-ui: ## Open Redis Commander in browser
	@echo "$(BLUE)[INFO]$(NC) Opening Redis Commander..."
	@command -v open >/dev/null && open http://localhost:8081 || echo "$(YELLOW)Open http://localhost:8081 manually$(NC)"

kafka-ui: ## Open Kafka UI in browser
	@echo "$(BLUE)[INFO]$(NC) Opening Kafka UI..."
	@command -v open >/dev/null && open http://localhost:8090 || echo "$(YELLOW)Open http://localhost:8090 manually$(NC)"

##@ Workflows

quick-start: ## Quick start for new developers (build and start everything)
	@echo "$(GREEN)[WORKFLOW]$(NC) Quick start for development..."
	@$(MAKE) clean
	@$(MAKE) build
	@$(MAKE) health
	@$(MAKE) ui

fresh: clean build health ## Clean slate: clean, build, and verify health

deploy-test: ## Deploy to test environment
	@echo "$(YELLOW)[DEPLOY]$(NC) Deploying to test environment..."
	@$(MAKE) stop ENV=test
	@$(MAKE) build ENV=test
	@$(MAKE) health ENV=test

full-reset: ## Complete reset including volumes and rebuild
	@echo "$(RED)[RESET]$(NC) Performing full reset..."
	@$(MAKE) clean-all
	@$(MAKE) build
	@$(MAKE) reset-db
	@$(MAKE) health

db-backup: ## Create database backups (development only)
	@echo "$(BLUE)[BACKUP]$(NC) Creating database backups..."
	@mkdir -p ./backups
	@docker-compose exec -T mysql mysqldump -u$${MYSQL_USER:-welcomer} -p$${MYSQL_PASSWORD:-devpassword} $${MYSQL_DATABASE:-welcomer_db} > ./backups/mysql-$$(date +%Y%m%d-%H%M%S).sql
	@docker-compose exec -T mongodb mongodump --username $${MONGO_INITDB_ROOT_USERNAME:-admin} --password $${MONGO_INITDB_ROOT_PASSWORD:-devpassword} --authenticationDatabase admin --out /tmp/backup
	@docker cp welcomer-mongodb:/tmp/backup ./backups/mongodb-$$(date +%Y%m%d-%H%M%S)/
	@echo "$(GREEN)[SUCCESS]$(NC) Backups created in ./backups/"

##@ Maintenance

clean: ## Clean up everything (WARNING: destroys all data and images!)
	@echo "$(RED)[WARNING]$(NC) This will clean up everything!"
	@./scripts/clean.sh -e $(ENV)

clean-all: ## Clean up everything including images
	@echo "$(RED)[WARNING]$(NC) This will clean up everything including images!"
	@./scripts/clean.sh -a -e $(ENV)

health: ## Check health of all services
	@echo "$(BLUE)[INFO]$(NC) Checking service health..."
	@curl -f http://localhost:8080/actuator/health 2>/dev/null && echo "$(GREEN)✓ App healthy$(NC)" || echo "$(RED)✗ App unhealthy$(NC)"
	@docker-compose ps

update: ## Pull latest images
	@echo "$(BLUE)[INFO]$(NC) Pulling latest images..."
	@docker-compose pull

##@ Gradle Tasks

gradle-build: ## Build the project with Gradle
	@echo "$(BLUE)[INFO]$(NC) Building project..."
	@./gradlew build

gradle-clean: ## Clean Gradle build directory
	@echo "$(BLUE)[INFO]$(NC) Cleaning build directory..."
	@./gradlew clean

gradle-test: ## Run all tests
	@echo "$(BLUE)[INFO]$(NC) Running tests..."
	@./gradlew test

gradle-check: ## Run code quality checks
	@echo "$(BLUE)[INFO]$(NC) Running code quality checks..."
	@./gradlew check

##@ Help

help: ## Display this help message
	@awk 'BEGIN {FS = ":.*##"; printf "\n$(BLUE)Welcomer SNS Feed System - Development Commands$(NC)\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(YELLOW)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(BLUE)Environment Options:$(NC)"
	@echo "  ENV=dev      Use development environment (default)"
	@echo "  ENV=test     Use test environment"  
	@echo "  ENV=staging  Use staging environment"
	@echo ""
	@echo "$(BLUE)Examples:$(NC)"
	@echo "  make start                    # Start all services"
	@echo "  make start SERVICE=\"mysql redis\" # Start specific services"
	@echo "  make logs SERVICE=app FOLLOW=true # Follow app logs"
	@echo "  make reset-db ENV=test        # Reset test databases"
	@echo "  make clean ENV=staging        # Clean staging environment"