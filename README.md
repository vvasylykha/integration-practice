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

### Implementation Steps

Participants should complete the following tasks in order:

#### Step 1: Configure Testcontainers
**File:** `src/test/java/com/example/exchange/integration/config/TestContainersConfig.java`

**Tasks:**
- Annotate the class as a Spring test configuration
- Declare static container fields for PostgreSQL, Redis, and RabbitMQ
- Start all containers in a static initializer block for shared usage across tests
- Annotate each `@Bean` method with `@ServiceConnection` for automatic Spring Boot configuration
- Implement the three container bean methods: `postgresContainer()`, `redisContainer()`, `rabbitMQContainer()`

#### Step 2: Configure Base Integration Test Class
**File:** `src/test/java/com/example/exchange/integration/base/ApiIntegrationTest.java`

**Tasks:**
- Annotate the class to start the full Spring application context on a random port
- Activate the "test" Spring profile
- Import `TestContainersConfig` to register containers
- Configure RestAssured in the `@BeforeEach` method to target the random server port
- Enable request/response logging on assertion failures

#### Step 3: Implement Balance Controller Tests
**File:** `src/test/java/com/example/exchange/integration/controller/BalanceControllerIT.java`

**Tasks:**
- Declare required repository fields for data cleanup
- Implement `@AfterEach` cleanup method to delete test data
- Implement 7 test methods using RestAssured Given-When-Then DSL:
  - `shouldDepositAndReturn201()` - POST deposit returns 201
  - `shouldGetAllBalancesForUser()` - GET all balances returns 200 with list
  - `shouldGetSpecificBalance()` - GET specific balance returns 200
  - `shouldReturn404WhenBalanceNotFound()` - GET non-existent balance returns 404
  - `shouldReturnEmptyListWhenUserHasNoBalances()` - GET returns empty array
  - `shouldReturn400OnInvalidCurrencyCode()` - Validation error for invalid currency
  - `shouldReturn400OnNegativeDepositAmount()` - Validation error for negative amount
  - `shouldReturn400WhenUserIdIsBlank()` - Validation error for blank userId

#### Step 4: Implement Rate Controller Tests
**File:** `src/test/java/com/example/exchange/integration/controller/RateControllerIT.java`

**Tasks:**
- Add `@WireMockTest` annotation to start WireMock server on port 8099
- Inject `CacheManager` for cache clearing
- Implement `@BeforeEach` method to clear cache entries
- Implement 6 test methods:
  - `shouldReturnExchangeRate()` - GET rate returns 200 with rate data
  - `shouldReturnAllRatesForBase()` - GET all rates returns map
  - `shouldCacheRateAndCallApiOnlyOnce()` - Verify caching behavior
  - `shouldReturn400OnInvalidBaseCode()` - Validation error handling
  - `shouldReturn400OnInvalidTargetCode()` - Validation error handling
  - `shouldRetryAndSucceedAfterTransientFailure()` - Retry mechanism with WireMock scenarios
  - `shouldReturn503AfterMaxRetryAttemptsExhausted()` - Failure after max retries

#### Step 5: Implement Exchange Controller Tests
**File:** `src/test/java/com/example/exchange/integration/controller/ExchangeControllerIT.java`

**Tasks:**
- Add `@WireMockTest` annotation to start WireMock server on port 8099
- Declare required repository fields
- Implement `@BeforeEach` method to stub external rate API and create initial balance
- Implement `@AfterEach` cleanup method (delete exchanges before balances)
- Implement 9 test methods:
  - `shouldPerformExchangeSuccessfullyAndReturn201()` - POST exchange returns 201
  - `shouldDeductBalanceAfterExchange()` - Verify balance deduction
  - `shouldReturn400WhenInsufficientBalance()` - Insufficient funds error
  - `shouldReturn400WhenSameCurrency()` - Same currency validation
  - `shouldReturn400OnValidationError()` - Bean validation errors
  - `shouldGetExchangeById()` - GET exchange by ID returns 200
  - `shouldReturn404WhenExchangeNotFound()` - GET non-existent exchange returns 404
  - `shouldGetUserExchangesPaginated()` - Pagination support
  - `shouldReturn503WhenExternalApiIsDown()` - External API failure handling

#### Step 6: Implement RabbitMQ Messaging Tests
**File:** `src/test/java/com/example/exchange/integration/messaging/ExchangeMessagingIT.java`

**Tasks:**
- Annotate the class to start Spring context without web server (no HTTP endpoints needed)
- Activate the "test" profile
- Import `TestContainersConfig` explicitly (this class does not extend `ApiIntegrationTest`)
- Inject `RabbitTemplate` and `AuditLogRepository` fields
- Implement `@AfterEach` cleanup method to delete all audit log records
- Implement 4 test methods:
  - `shouldCreateAuditLogWhenExchangeCompletedEventReceived()` - Verify audit log creation with event type "COMPLETED"
  - `shouldCreateAuditLogWhenExchangeFailedEventReceived()` - Verify audit log creation with event type "FAILED"
  - `shouldBeIdempotentWhenDuplicateCompletedEventReceived()` - Verify idempotency (at-least-once delivery), use Awaitility
  - `shouldBeIdempotentWhenDuplicateFailedEventReceived()` - Verify idempotency for failed events, use Awaitility

#### Step 7: Implement Repository Tests
**File:** `src/test/java/com/example/exchange/integration/repository/UserBalanceRepositoryIT.java`

**Tasks:**
- Use Spring Boot JPA test slice annotation (loads only JPA components, not full context)
- Disable auto-replacement of datasource with in-memory database (required for pessimistic locking with real PostgreSQL)
- Activate the "test" profile
- Import `TestContainersConfig`
- Inject `UserBalanceRepository` field
- Implement 8 test methods (each test runs in auto-rollback transaction):
  - `shouldFindBalanceByUserIdAndCurrency()` - Find balance returns present Optional
  - `shouldReturnEmptyWhenBalanceNotFound()` - Find non-existent balance returns empty Optional
  - `shouldFindAllBalancesForUser()` - Find all balances for user with multiple currencies
  - `shouldReturnEmptyListWhenUserHasNoBalances()` - Find all returns empty list
  - `shouldAcquirePessimisticLockForUpdate()` - Test pessimistic write lock (SELECT ... FOR UPDATE)
  - `shouldReturnEmptyWhenLockingNonExistentBalance()` - Pessimistic lock on non-existent record
  - `shouldSaveAndIncrementVersionOnUpdate()` - Verify optimistic locking with @Version (0 -> 1)
  - `shouldCheckExistenceByUserIdAndCurrency()` - Test existence check method

#### Step 8: Implement Flyway Migration Tests
**File:** `src/test/java/com/example/exchange/integration/migration/FlywayMigrationIT.java`

**Tasks:**
- Annotate the class to start Spring context without web server
- Activate the "test" profile
- Import `TestContainersConfig`
- Inject `Flyway` bean to inspect migration metadata
- Implement 3 test methods:
  - `shouldApplyAllMigrationsSuccessfully()` - Verify at least one migration applied with state SUCCESS
  - `shouldHaveNoPendingMigrations()` - Verify no unapplied migrations after context startup
  - `shouldNotHaveFailedMigrations()` - Verify no migrations in FAILED state

#### Step 9: Run All Tests

Execute the complete test suite:

```bash
mvn test
```

Verify that all integration tests pass successfully.

## Key Learning Objectives

- Configure Testcontainers for integration testing with real database, cache, and message broker
- Use `@ServiceConnection` for automatic Spring Boot configuration
- Implement REST API tests with RestAssured
- Mock external HTTP dependencies with WireMock
- Test caching behavior with Redis
- Verify retry mechanisms and error handling
- Test asynchronous messaging with RabbitMQ
- Implement proper test data cleanup strategies
- Handle pagination and validation in integration tests