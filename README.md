# Integration Testing Practice

This is an educational project from the Codeus community for learning integration testing practices. This repository is part of the **1-4-integration-test-scratch** section.

## Prerequisites

- Java 21
- Maven 3.6+
- Docker and Docker Compose
- Postman (optional, for manual API testing)

## Getting Started

### 1. Start Infrastructure Services

Before running the Spring Boot application, start the required infrastructure services using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5434)
- Redis (port 6380)
- RabbitMQ (ports 5672, 15672)

### 2. Run the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080.

### 3. Import Postman Collection

Import the `1-4-integration-test-scratch.postman_collection.json` file into Postman to access pre-configured API request templates for manual testing.

## Project Architecture

### Architecture Diagram

The overall component architecture is shown below (rendered from the PlantUML source in
`diagrams/component_diagram.puml`). It shows the full request and event flow across every layer —
controllers → services → repositories / API client / event publisher → listener — together with the
backing infrastructure: PostgreSQL, the Redis rates cache, the RabbitMQ audit exchange/queues and the
external exchange-rate API.

![Component Architecture](diagrams/png/Component%20Architecture.png)

Each test class in the practice below also links to a **focused slice** of this diagram that highlights
only the components that class exercises, so you can see exactly which part of the system each
integration test covers.

### Technology Stack

- **Spring Boot 3.5.6**
- **PostgreSQL 16**
- **Redis 7**
- **RabbitMQ 3.13**
- **Flyway**

### API Endpoints

**Balance Management**
- `POST /api/v1/balances/deposit` - Deposit funds to user balance
    - Request: `DepositRequest` (userId, currency, amount)
    - Response: `BalanceResponse` (HTTP 201 Created)
    - Validation: userId not blank, currency 3-letter code, amount positive
- `GET /api/v1/balances/{userId}` - Get all balances for user
    - Response: List of `BalanceResponse` (HTTP 200 OK, empty array if no balances)
- `GET /api/v1/balances/{userId}/{currency}` - Get specific balance
    - Response: `BalanceResponse` (HTTP 200 OK)
    - Error: HTTP 404 if balance not found

**Exchange Rates**
- `GET /api/v1/rates/{base}` - Get all exchange rates for base currency
    - Response: Map of currency codes to rates (HTTP 200 OK)
    - Cached in Redis with key pattern: `rates-all:{baseCurrency}`
- `GET /api/v1/rates/{base}/{target}` - Get specific exchange rate
    - Response: `RateResponse` (baseCurrency, targetCurrency, rate, timestamp)
    - Cached in Redis with key pattern: `rates:{base}:{target}`
    - Validation: both base and target must be 3-letter currency codes

**Currency Exchange**
- `POST /api/v1/exchanges` - Perform currency exchange
    - Request: `ExchangeRequest` (userId, fromCurrency, toCurrency, amount)
    - Response: `ExchangeResponse` (HTTP 201 Created)
    - Business logic: deducts amount + commission from source currency, adds converted amount to target currency
    - Commission rate: 0.5% (configurable via `exchange.commission.rate`)
    - Publishes `ExchangeCompletedEvent` to RabbitMQ after successful exchange
- `GET /api/v1/exchanges/{id}` - Get exchange by ID
    - Response: `ExchangeResponse` (HTTP 200 OK)
    - Error: HTTP 404 if exchange not found
- `GET /api/v1/exchanges/user/{userId}` - Get user exchanges with pagination
    - Query params: `page` (default: 0), `size` (default: 20)
    - Response: List of `ExchangeResponse` (HTTP 200 OK)


### Core Components

**Database (PostgreSQL)**

Tables:
- `user_balances` - Stores user balances per currency
  - Fields: id, user_id, currency (3-letter code), balance, locked_balance, updated_at, version
  - Optimistic locking with `@Version` to prevent concurrent modification issues
  - Composite unique constraint on (user_id, currency)
- `exchanges` - Records all exchange transactions
  - Fields: id, user_id, from_currency, to_currency, amount, converted_amount, exchange_rate, commission, status (PENDING/COMPLETED/FAILED), created_at, updated_at
  - Foreign key relationship to user_balances
- `audit_log` - Stores audit records of exchange events received from RabbitMQ
  - Fields: id, exchange_id, event_type (VARCHAR 50), user_id, details (TEXT), ip_address, user_agent, processed_at
  - Unique constraint on (exchange_id, event_type) to prevent duplicate event processing
  - Indexes on: exchange_id, user_id, event_type for efficient querying

**Cache (Redis)**

Configuration (`RedisConfig`):
- TTL: 5 minutes (300,000 ms) for all cache entries
- Serialization: StringRedisSerializer for keys, GenericJackson2JsonRedisSerializer for values
- Cache names:
  - `rates` - Individual exchange rates (key: `{base}:{target}`)
  - `rates-all` - All rates for base currency (key: `{baseCurrency}`)

Caching strategy (`RateService`):
- `@Cacheable` on `getExchangeRate()` and `getAllRatesForBase()` methods
- `@CacheEvict` on `clearCache()` method to invalidate all rate caches
- Reduces external API calls by caching frequently requested rates

**Message Broker (RabbitMQ)**

Configuration (`RabbitMQConfig`):
- Exchange: `exchange-audit-exchange` (TopicExchange)
- Queues:
  - `exchange-audit-completed-queue` - Receives completed exchange events
  - `exchange-audit-failed-queue` - Receives failed exchange events
  - `exchange-audit-dlq` - Dead letter queue for failed message processing
- Routing keys:
  - `audit.completed` - Routes to completed queue
  - `audit.failed` - Routes to failed queue
  - `audit.dead` - Routes to dead letter queue
- Dead Letter Exchange (DLX): `exchange-audit-dlx` configured for automatic retry handling
- Message converter: Jackson2JsonMessageConverter for JSON serialization

Event publishing (`RabbitExchangeEventPublisher`):
- Publishes `ExchangeCompletedEvent` after successful currency exchange
- Event contains: exchangeId, userId, fromCurrency, toCurrency, amount, convertedAmount, exchangeRate, commission, timestamp
- Enables asynchronous audit logging and potential downstream processing

**External API Integration**

Client (`ExchangeRateApiClient`):
- Uses Spring `RestClient` to fetch exchange rates from external API
- Base URL: configurable via `exchange.api.url` (production: https://api.exchangerate-api.com/v4/latest)
- Timeout: 5 seconds (configurable via `exchange.api.timeout`)

Retry mechanism:
- `@Retryable` annotation with exponential backoff
- Max attempts: 3 (configurable via `exchange.api.retry.max-attempts`)
- Backoff strategy: initial delay 1000ms, multiplier 2 (1s, 2s, 4s)
- Retries on: `RestClientException`
- `@Recover` method throws `ExchangeRateNotFoundException` after all retries exhausted

## Test Structure

The test suite is located in `src/test` and follows a structured approach to integration testing.

### Testing Technologies

- **JUnit 5** - Testing framework
- **Spring Boot Test** - Spring context integration
- **Testcontainers** - Containerized dependencies (PostgreSQL, Redis, RabbitMQ)
- **RestAssured** - REST API testing DSL
- **WireMock** - External API mocking
- **Awaitility** - Asynchronous testing utilities

## Practice: Integration Testing Best Practices

### Prerequisite: Docker must be running

Every test class uses **Testcontainers**, which starts PostgreSQL, Redis and RabbitMQ in Docker on
demand. **Start Docker (Docker Desktop / the Docker daemon) before running any test** — otherwise the
containers cannot be created and the tests fail at startup. You do **not** need to run
`docker-compose up` for the tests: Testcontainers manages those containers itself (the Compose file is
only for running the application manually).

Run the suite with:

```bash
mvn test
```

### What this is

The suite intentionally violates four best practices (BP1–BP4), and many test methods are left as
`// TODO` stubs. Working class by class, you fix the violation and implement the tests. Each stub is
marked **REQUIRED** or **OPTIONAL**:

- **REQUIRED** — the baseline tests every real suite is expected to have (happy path + the core
  error/validation cases). Do these first.
- **OPTIONAL** — broaden coverage to edge cases, caching, retries and resilience.

### Recommended order

Work through the classes in the order below. It climbs from the simplest layer (persistence) to the
most complex (asynchronous messaging), adding one technology at a time. The basic scenario is: complete
the **REQUIRED** tests of each class in this order, so you cover the core behaviour of every component
before moving on to harder technologies and the OPTIONAL cases.

**1. `ExchangeRepositoryIT`** — BP4 (Execution Speed & Context Optimization)
- *Stack:* JPA repository + Testcontainers PostgreSQL.
- *Diagram:* [`ExchangeController Component Architecture.png`](diagrams/png/ExchangeController%20Component%20Architecture.png) — exercises the Data Access slice only (`ExchangeRepository → exchanges`).

**2. `UserBalanceRepositoryIT`** — BP2 (Infrastructure Realism)
- *Stack:* JPA repository + PostgreSQL-specific SQL.
- *Diagram:* [`BalanceController Component Architecture.png`](diagrams/png/BalanceController%20Component%20Architecture.png) — exercises the persistence slice only (`UserBalanceRepository → user_balances`).

**3. `BalanceControllerIT`** — BP1 (Test Isolation & Independence)
- *Stack:* full web context + RestAssured (HTTP) + PostgreSQL.
- *Diagram:* [`BalanceController Component Architecture.png`](diagrams/png/BalanceController%20Component%20Architecture.png) — `BalanceController → BalanceService → UserBalanceRepository → user_balances`.

**4. `RateControllerIT`** — BP1 (Redis cache) + BP3 (Stable External Boundaries)
- *Stack:* web + Redis cache + WireMock.
- *Diagram:* [`RateController Integration Scope.png`](diagrams/png/RateController%20Integration%20Scope.png) — `RateController → RateService → ExchangeRateApiClient → external API`, with the Redis rates cache.

**5. `ExchangeControllerIT`** — BP1 (Test Isolation) + BP3 (Stable External Boundaries)
- *Stack:* web + PostgreSQL + WireMock (external rate API).
- *Diagram:* [`ExchangeController Component Architecture.png`](diagrams/png/ExchangeController%20Component%20Architecture.png) — the full exchange flow: `ExchangeController → services → ExchangeRepository`, plus the published audit event.

**6. `ExchangeMessagingIT`** — BP1 (async timing & idempotency)
- *Stack:* RabbitMQ messaging + Awaitility (async).
- *Diagram:* [`ExchangeController Component Architecture.png`](diagrams/png/ExchangeController%20Component%20Architecture.png) — the Messaging slice (`RabbitMQ → ExchangeEventListener → AuditService → AuditLogRepository → audit_log`).

### Definition of done

All integration tests pass, stay green across re-runs and random order (`-Dsurefire.runOrder=random`),
run fully offline (no live API — WireMock instead), and use real PostgreSQL via Testcontainers.

**Reference:** see slides 11–22 for the problem/fix pattern of each practice.