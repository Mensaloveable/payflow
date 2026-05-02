# PayFlow — Payment Processor

A Spring Boot application demonstrating the **Strategy Design Pattern** applied to a multi-method payment processing system. Transactions are persisted in **PostgreSQL**, reads are cached in **Redis**, and the UI is built with **Thymeleaf**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Language | Java 17 |
| Architecture | Strategy Design Pattern |
| Persistence | PostgreSQL 16 + Spring Data JPA |
| Caching | Redis 7 + Spring Cache (`@Cacheable` / `@CacheEvict`) |
| Rate Limiting | Redis sliding window counter |
| Migrations | Flyway |
| UI | Thymeleaf |
| Build | Maven |
| Infrastructure | Docker + Docker Compose |

---

## Payment Methods

| Method | Strategy Class |
|---|---|
| Credit Card | `CreditCardPaymentStrategy` |
| PayPal | `PayPalPaymentStrategy` |
| Cryptocurrency | `CryptocurrencyPaymentStrategy` |
| Bank Transfer | `BankTransferPaymentStrategy` |
| USSD Mobile Money | `USSDPaymentStrategy` |

---

## Architecture

The Strategy Pattern decouples payment algorithms from the code that uses them. Each payment method is a self-contained `@Component` that implements the `PaymentStrategy` interface. `PaymentContext` auto-discovers all strategies at startup via Spring DI and selects the right one at runtime based on the user's request.

```
HTTP Request
    │
    ▼
PaymentController
    │
    ▼
PaymentService
    ├── RateLimiterService ──► Redis INCR (10 req/min, 100 req/hr)
    ├── PaymentContext     ──► resolves strategy by PaymentMethod enum
    │       └── PaymentStrategy (interface)
    │               ├── CreditCardPaymentStrategy
    │               ├── PayPalPaymentStrategy
    │               ├── CryptocurrencyPaymentStrategy
    │               ├── BankTransferPaymentStrategy
    │               └── USSDPaymentStrategy
    ├── TransactionRepository ──► PostgreSQL
    └── @CacheEvict           ──► invalidates Redis on every write

GET requests:
    ├── @Cacheable("recent-transactions")  TTL: 30s
    ├── @Cacheable("payment-stats")        TTL: 60s
    ├── @Cacheable("transaction-detail")   TTL: 10m
    └── @Cacheable("method-stats")         TTL: 5m
```

### Strategy Pattern — How Spring Selects a Strategy

Spring does not pick the strategy — `PaymentContext` does. On startup, Spring injects all `PaymentStrategy` beans into a `List`, which `PaymentContext` converts into a `Map<PaymentMethod, PaymentStrategy>`. At runtime, the `paymentMethod` value from the user's request is used as the map key to look up and invoke the correct strategy.

```java
// PaymentContext — self-registering strategy registry
public PaymentContext(List<PaymentStrategy> strategies) {
    this.strategyRegistry = strategies.stream()
        .collect(Collectors.toMap(PaymentStrategy::getPaymentMethod, s -> s));
}

// Runtime dispatch — no if/else, no switch
PaymentStrategy strategy = strategyRegistry.get(request.getPaymentMethod());
return strategy.process(request);
```

Adding a new payment method requires only one thing: a new `@Component` class that implements `PaymentStrategy`. Nothing else in the codebase changes.

---

## Project Structure

```
src/
└── main/
    ├── java/com/payments/
    │   ├── PaymentProcessorApplication.java
    │   ├── cache/
    │   │   └── RateLimiterService.java       # Redis sliding window rate limiter
    │   ├── config/
    │   │   └── RedisConfig.java              # Redis template, cache manager, TTLs
    │   ├── controller/
    │   │   └── PaymentController.java        # Web + REST endpoints
    │   ├── entity/
    │   │   └── Transaction.java              # JPA entity → transactions table
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java   # Rate limit + error handling
    │   ├── model/
    │   │   ├── PaymentMethod.java            # Enum: CREDIT_CARD, PAYPAL, etc.
    │   │   ├── PaymentRequest.java           # Form input DTO
    │   │   ├── PaymentResult.java            # Strategy output DTO
    │   │   └── PaymentStats.java             # Dashboard stats DTO
    │   ├── repository/
    │   │   └── TransactionRepository.java    # Spring Data JPA repository
    │   ├── service/
    │   │   ├── PaymentContext.java           # Strategy registry + dispatcher
    │   │   └── PaymentService.java           # Orchestration, caching, persistence
    │   └── strategy/
    │       ├── PaymentStrategy.java          # Strategy interface
    │       ├── CreditCardPaymentStrategy.java
    │       ├── PayPalPaymentStrategy.java
    │       ├── CryptocurrencyPaymentStrategy.java
    │       ├── BankTransferPaymentStrategy.java
    │       └── USSDPaymentStrategy.java
    └── resources/
        ├── application.properties
        ├── db/migration/
        │   ├── V1__create_transactions_table.sql
        │   └── V2__add_stats_view_and_daily_summary.sql
        └── templates/
            └── index.html                    # Thymeleaf UI
```

---

## Database Schema

Flyway applies migrations automatically on startup. No manual setup needed.

```sql
-- V1: Core ledger table
transactions (
    id                BIGSERIAL PRIMARY KEY,
    transaction_id    VARCHAR(64) UNIQUE,
    payment_method    VARCHAR(32),
    status            VARCHAR(16),        -- COMPLETED | FAILED
    success           BOOLEAN,
    amount            NUMERIC(19,4),
    currency          VARCHAR(8),
    recipient         VARCHAR(255),
    description       VARCHAR(512),
    message           VARCHAR(512),
    processor_details TEXT,
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ         -- auto-updated by DB trigger
)

-- V2: Analytics
payment_method_stats     -- VIEW: live aggregates per payment method
daily_transaction_summary -- TABLE: pre-aggregated daily rollups
```

---

## Redis Cache Keys

| Cache Name | Key Pattern | TTL | Evicted On |
|---|---|---|---|
| `payment-stats` | `payment-stats::global` | 60s | Every payment |
| `recent-transactions` | `recent-transactions::page:0:size:20` | 30s | Every payment |
| `transaction-detail` | `transaction-detail::{TXN-ID}` | 10m | Never (immutable) |
| `method-stats` | `method-stats::breakdown` | 5m | Every payment |
| `rate_limit:minute:{ip}` | — | 60s | Auto-expires |
| `rate_limit:hour:{ip}` | — | 3600s | Auto-expires |

---

## REST Endpoints

| Method | Path | Description | Cached |
|---|---|---|---|
| `GET` | `/` | Main UI | — |
| `POST` | `/process` | Submit a payment | Evicts caches |
| `GET` | `/transactions` | Paginated transaction list (JSON) | Yes — 30s |
| `GET` | `/transactions/{id}` | Single transaction by TXN-ID (JSON) | Yes — 10m |
| `GET` | `/stats` | Aggregate stats (JSON) | Yes — 60s |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker + Docker Compose

---

## Running Locally

**1. Start PostgreSQL and Redis**

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on `localhost:5432` (database: `payflow`)
- Redis on `localhost:6379`
- Redis Commander UI on `localhost:8081`

**2. Run the application**

```bash
mvn spring-boot:run
```

Flyway automatically applies `V1__` and `V2__` migrations on first boot. The app starts on `http://localhost:8080`.

**3. Useful URLs**

| URL | Description |
|---|---|
| `http://localhost:8080` | Payment UI |
| `http://localhost:8081` | Redis Commander — inspect live cache keys |

---

## Configuration

All configuration lives in `src/main/resources/application.properties`.

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/payflow
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Suppress favicon 404 noise
spring.mvc.favicon.enabled=false
```

---

## Adding a New Payment Method

1. Add the value to the `PaymentMethod` enum:
```java
public enum PaymentMethod {
    CREDIT_CARD, PAYPAL, CRYPTOCURRENCY, BANK_TRANSFER, USSD,
    APPLE_PAY  // ← new
}
```

2. Create a strategy class:
```java
@Component
public class ApplePayPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.APPLE_PAY;
    }

    @Override
    public String validate(PaymentRequest request) { ... }

    @Override
    public PaymentResult process(PaymentRequest request) { ... }
}
```

3. Add the form fields to `index.html`.

That's it. `PaymentContext` auto-discovers the new bean on the next startup. No other code changes.

---

## Rate Limiting

Requests are rate-limited per client IP using a Redis sliding window counter.

| Window | Limit |
|---|---|
| Per minute | 10 requests |
| Per hour | 100 requests |

If the limit is exceeded, the UI displays a warning and the request is rejected before reaching the payment strategy. The limiter **fails open** — if Redis is unavailable, payments are not blocked.







[//]: # (OBSOLUTE).
# PayFlow — Payment Processor v2
### Spring Boot · Strategy Pattern · PostgreSQL · Redis

---

## Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Language | Java 17 |
| Persistence | PostgreSQL 16 + Spring Data JPA |
| Cache / Rate Limit | Redis 7 + Spring Cache |
| Migrations | Flyway |
| UI | Thymeleaf |
| Build | Maven |

---

## Architecture

```
HTTP Request
    │
    ▼
PaymentController
    │
    ▼
PaymentService
    ├── RateLimiterService  ──► Redis INCR (10 req/min sliding window)
    ├── PaymentContext       ──► selects strategy at runtime
    │       └── PaymentStrategy (interface)
    │               ├── CreditCardPaymentStrategy
    │               ├── PayPalPaymentStrategy
    │               ├── CryptocurrencyPaymentStrategy
    │               ├── BankTransferPaymentStrategy
    │               └── USSDPaymentStrategy
    ├── TransactionRepository ─► PostgreSQL (transactions table)
    └── @CacheEvict          ──► invalidates Redis on write

GET requests:
    PaymentService
        ├── @Cacheable("recent-transactions")  TTL: 30s
        ├── @Cacheable("payment-stats")        TTL: 60s
        └── @Cacheable("method-stats")         TTL: 5m
```

## Redis Cache Keys

| Cache | TTL | Content |
|---|---|---|
| `payment-stats::global` | 60s | Aggregate totals (count, volume, success rate) |
| `recent-transactions::page:0:size:20` | 30s | Latest 20 transactions |
| `transaction-detail::{TXN-ID}` | 10m | Single transaction (immutable) |
| `method-stats::breakdown` | 5m | Per-method counts |
| `rate_limit:minute:{ip}` | 60s | Request count per client/minute |
| `rate_limit:hour:{ip}` | 3600s | Request count per client/hour |

## Database Schema

```sql
-- V1: Core transactions table
transactions (
    id, transaction_id (unique), payment_method,
    status, success, amount, currency, recipient,
    description, message, processor_details,
    created_at, updated_at
)

-- V2: Analytics view + daily rollup
payment_method_stats  (VIEW — live aggregates)
daily_transaction_summary  (TABLE — pre-aggregated rollups)
```

---

## Running Locally

### 1. Start infrastructure
```bash
docker-compose up -d
```

Starts PostgreSQL on `:5432`, Redis on `:6379`, Redis Commander UI on `:8081`.

### 2. Run the application
```bash
mvn spring-boot:run
```

Flyway automatically applies `V1__` and `V2__` migrations on first boot.

### 3. Open the UI
```
http://localhost:8080        — Payment UI
http://localhost:8081        — Redis Commander (inspect cache keys live)
```

### 4. REST endpoints
```
GET  /stats                  — JSON: aggregate stats (Redis cached)
GET  /transactions           — JSON: paginated transaction list
GET  /transactions/{TXN-ID}  — JSON: single transaction
```

---

## Adding a New Payment Method

1. Add the value to `PaymentMethod` enum
2. Create `YourStrategy implements PaymentStrategy` annotated `@Component`
3. Add a field group in `index.html`

**That's it.** `PaymentContext` auto-discovers the new bean. No other changes.

---

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/payflow
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```
# payflow
