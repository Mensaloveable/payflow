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
