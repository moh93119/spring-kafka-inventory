# Distributed Inventory System

Two Spring Boot services choreographed over Kafka, fronted by a Spring
Cloud Gateway - the direct sequel to
[`dotnet-to-spring-orders`](../dotnet-to-spring-orders), whose outbox
dispatcher stops at logging what it would publish. This repo is where that
outbox pattern gets a **real** Kafka producer, on both sides of a genuine
two-way async choreography, plus Redis, Resilience4j, Actuator/Micrometer,
Docker Compose and Kubernetes manifests.

**Closing a flagged gap.** [The migration plan](../plan.md)'s messaging
phase teaches Spring AMQP/RabbitMQ, not Kafka - Kafka is explicitly named
a deferred "gap to close" in the capstone phase ("Spring for Apache Kafka
mirrors Spring AMQP closely, so it is a short hop once Phase 5 is done").
This repo is that short hop: the topology-as-beans philosophy, the
poison-message/DLQ handling, and the Jackson serialization-safety warning
already learned for RabbitMQ all reappear here in their Kafka form. Redis,
Spring Cloud Gateway ("your Ocelot") and Resilience4j (mapped 1:1 to
Polly) *are* covered by the plan, and this repo uses that exact vocabulary.

## Architecture

```
                        ┌──────────────┐
   client ────────────► │  api-gateway │  routing, rate limiting,
                        │ (WebFlux)    │  circuit breaker
                        └──────┬───────┘
                   ┌───────────┴───────────┐
                   ▼                       ▼
         ┌──────────────────┐    ┌──────────────────────┐
         │ inventory-service│    │ reservation-service   │
         │  Stock, Redis    │    │  Reservation           │
         └────────┬─────────┘    └───────────┬────────────┘
                   │   outbox → Kafka →  inbox │
                   │◄────── inventory-events ──┤
                   ├── reservation-requests ──►│
                   ▼                            ▼
              Postgres (inventory)        Postgres (reservation)
```

**Flow**: `POST /api/reservations` (via the gateway) creates a `PENDING`
reservation in reservation-service and writes a `ReservationRequestedEvent`
to its outbox. A `@Scheduled` dispatcher publishes it to
`reservation-requests`. inventory-service consumes it idempotently, tries
to reserve stock, and writes `InventoryEventMessage` (RESERVED or
REJECTED) to its own outbox, dispatched to `inventory-events`.
reservation-service consumes that idempotently and resolves the
reservation to `CONFIRMED`/`REJECTED`. `GET /api/reservations/{id}` shows
the eventual result - the whole round trip is asynchronous end to end.

## The outbox/inbox pattern, on both sides

Both services reuse the same trio as `dotnet-to-spring-orders`
(`OutboxMessageEntity`/`OutboxWriter`/`KafkaOutboxDispatcher`), pointed at
a real `KafkaTemplate.send(topic, key, payload)` this time. The outbox
already stores each event pre-serialized to JSON, so the producer's
`value-serializer` is plain `StringSerializer`, not `JsonSerializer` -
using `JsonSerializer` on an already-JSON string would re-encode it as a
quoted string literal and corrupt the payload (a real bug caught while
building this).

The inbox side (`IdempotencyGuard`/`processed_events`) is the necessary
counterpart: Kafka only guarantees at-least-once delivery, so every
consumer records the event id in the same transaction as its business
change and skips anything it's already seen.
`ReservationChoreographyIntegrationTest`/`InventoryEventChoreographyIntegrationTest`
publish the same event twice and assert the side effect happens once.

## Dead-letter topics

Both `@KafkaListener` methods carry
`@RetryableTopic(attempts = "4", backoff = @Backoff(delay = 500, multiplier = 2))`
plus a `@DltHandler`. A message that can never be processed exhausts the
auto-created retry topics and lands on `<topic>.dlt` instead of blocking
the partition forever or looping silently -
`DeadLetterTopicIntegrationTest` in each service proves it by publishing
an unparseable payload and asserting a record eventually appears on the
DLT.

## Redis + Resilience4j

`StockQueryService` reads through Redis cache-aside
(`StringRedisTemplate`, not `@Cacheable` - combining declarative caching
and a Resilience4j annotation on the same method has real proxy-ordering
subtlety not worth the risk here). `@Retry` then `@CircuitBreaker` wrap
the Redis read specifically, falling back to Postgres
(`getStockFromDb(sku, Throwable)` - the Resilience4j fallback signature)
when Redis is flapping. A cache miss is not a failure and never reaches
either annotation; they only engage when Redis itself throws.

## Gateway

`api-gateway`'s `application.yml` routes `/api/stock/**` and
`/api/reservations/**` to the two services, each with a `RequestRateLimiter`
(Redis-backed, keyed per client IP) and a `CircuitBreaker` filter whose
`fallbackUri` lands on `FallbackController` (a 503 `ProblemDetail`). The
reactive `CircuitBreaker` filter is configured in Java
(`CircuitBreakerConfiguration`), not via `resilience4j.circuitbreaker.instances.*`
YAML - that property style belongs to the non-reactive
`resilience4j-spring-boot3` autoconfiguration used inside inventory-service,
a different integration from Spring Cloud Gateway's reactive one.

## Actuator/Micrometer

All three services expose `/actuator/health` (liveness/readiness split)
and `/actuator/prometheus`. `docker-compose.yml` includes a Prometheus
service scraping all three - Grafana dashboards on top would be a natural
next step but aren't included here.

## Kubernetes

`k8s/` has a Deployment + Service + ConfigMap per app service, readiness/
liveness probes wired to the Actuator probe endpoints. It assumes
Postgres/Redis/Kafka already exist in-cluster (e.g. via Bitnami Helm
charts) - not included here, same as any real deployment.

## Running it

```bash
docker compose up -d --build
curl -X POST localhost:8080/api/reservations \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1","quantity":2}'
# first seed some stock directly against inventory-service:
curl -X POST localhost:8081/api/stock/SKU-1/adjust \
  -H 'Content-Type: application/json' \
  -d '{"warehouseId":"WH-1","delta":10}'
curl localhost:8080/api/reservations/{id}   # poll until CONFIRMED/REJECTED
```

## Testing

```bash
./mvnw test     # unit + slice tests, all three modules, no Docker required
./mvnw verify   # + Testcontainers integration tests (Kafka + Postgres + Redis, requires Docker)
```

Automated integration coverage is scoped per module (outbox-actually-
publishes, idempotent double-delivery, DLT-after-retries-exhausted) rather
than one cross-module test spanning both services - Maven modules test
their own code, and the true end-to-end choreography (place a reservation
through the gateway, poll until resolved) is the `docker compose`
walkthrough above rather than an automated JUnit test.

## A note on the local build/verification environment

Same corporate-network fixes as `dotnet-to-spring-orders` (Fortinet
TLS-inspection proxy needing a patched `cacerts` copy, Lombok pinned to
1.18.48 for JDK 24) apply here too. Two more came up specifically for this
repo:

- **Docker Hub unreachable from this network** - images were pulled
  through a local mirror (`focker.ir/<image>`) instead. Testcontainers'
  `PostgreSQLContainer`/`KafkaContainer`/`RedisContainer` validate the
  image repository against their expected family, so a mirror-prefixed
  name needs `.asCompatibleSubstituteFor("postgres")` /
  `"apache/kafka"` / `"redis"` - the image name itself is overridable per
  test via `-Dtestcontainers.<postgres|kafka|redis>.image=...` system
  properties, defaulting to the plain Docker Hub name for anyone building
  this on a network where that just works.
- **docker-java (bundled with this Testcontainers version) defaults to
  Docker Engine API 1.32**, which Docker Engine 29.x rejects
  (`client version 1.32 is too old. Minimum supported API version is
  1.40`). `inventory-service`/`reservation-service`'s `pom.xml` pin
  `DOCKER_API_VERSION=1.41` via Failsafe's `<environmentVariables>` -
  confirmed (via `mvnw -X`) to actually reach the forked test JVM's
  environment, but docker-java still isn't picking it up through
  whichever code path Testcontainers' `DockerClientProviderStrategy` uses
  here. `./mvnw test` (fast suite, no Docker) is fully green in all three
  modules; `./mvnw verify`'s Testcontainers tests are correct and reviewed
  but still blocked locally by this specific version mismatch - the same
  class of local-environment issue flagged in `dotnet-to-spring-orders`'s
  README, now narrowed down further but not yet resolved.
