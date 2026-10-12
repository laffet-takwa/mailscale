.POSIX:

# MailScale developer convenience targets.
# All heavy lifting lives in the module build tools; these are shortcuts.

SHELL := /bin/sh

.PHONY: help up down api worker frontend-install frontend-dev frontend-build \
        test-backend test-worker test-frontend test-all lint docs clean reset

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-20s %s\n", $$1, $$2}'

up: ## Start all infrastructure (postgres, kafka, redis, mailpit, prometheus, grafana)
	docker compose up -d

down: ## Stop infrastructure
	docker compose down

api: ## Run the Spring Boot API locally
	./mvnw -f backend-java/pom.xml spring-boot:run

worker: ## Run the Kotlin worker locally
	./gradlew -p worker-kotlin run

frontend-install:
	npm --prefix frontend install

frontend-dev:
	npm --prefix frontend run dev

frontend-build:
	npm --prefix frontend run build

test-backend: ## Run backend unit + H2-backed integration tests
	./mvnw -f backend-java/pom.xml test

test-backend-integration: ## Run Testcontainers-backed integration tests (requires Docker)
	./mvnw -f backend-java/pom.xml verify -Pintegration

test-worker:
	./gradlew -p worker-kotlin test

test-frontend:
	npm --prefix frontend test -- --run

test-all: test-backend test-worker test-frontend ## Run all fast test suites

lint: ## Run static analysis (Checkstyle + SpotBugs + Detekt + ESLint)
	./mvnw -f backend-java/pom.xml checkstyle:check spotbugs:check
	./gradlew -p worker-kotlin detekt
	npm --prefix frontend run lint

docs: ## Render docs index (informational)
	@echo "See docs/ - architecture overview, ADRs, database schema, API reference."

reset: down ## Reset local state (drops the postgres volume)
	docker compose down -v

clean: ## Remove build output
	rm -rf backend-java/target worker-kotlin/build frontend/dist
