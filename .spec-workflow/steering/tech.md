# Technology Stack

## Project Type
Spring Boot 3.5.x modular monolith using Spring Modulith. Provides REST endpoints and AMQP integration with RabbitMQ. Demonstrates application events, event externalization, and inbound adapters.

## Core Technologies

### Primary Language(s)
- Language: Java 21
- Build: Maven (wrapper `./mvnw`)

### Key Dependencies/Libraries
- Spring Boot: 3.5.5 (parent)
- Spring Modulith: 1.4.3 (BOM-managed)
  - `spring-modulith-starter-core` — Modulith core support
  - `spring-modulith-events-api` — Application Events API
  - `spring-modulith-events-amqp` — Event externalization to RabbitMQ
  - `spring-modulith-events-jackson` — JSON serialization for events
  - `spring-modulith-starter-jdbc` — JDBC Event Publication Registry
- Spring Web: `spring-boot-starter-web`
- Validation: `spring-boot-starter-validation`
- Spring AMQP: `spring-rabbit`
- Database (dev): H2 runtime (for JDBC Event Publication Registry)
- Test: `spring-boot-starter-test`, `spring-modulith-starter-test`

### Application Architecture
- Modular monolith with clear boundaries:
  - `domain/order`: Core domain + REST controllers, services, domain events (e.g., `OrderCompleted`)
  - `feature/inventory`: Listeners reacting to domain events using `@ApplicationModuleListener`
  - `inbound/amqp`: RabbitMQ topology and listeners (e.g., `new-orders` inbound adapter)
- Integration style:
  - Cross-module interactions exclusively via application events
  - Event externalization to AMQP via `@Externalized`
  - Inbound AMQP via `@RabbitListener` translating messages to internal events
- Reliability:
  - Event Publication Registry (JDBC) ensures at-least-once processing for transactional event listeners
  - Optional republish on restart and completion modes configurable

### Data Storage
- Primary: In-memory H2 used for Event Publication Registry and demo persistence if needed
- Data formats: JSON (Jackson) for event serialization and REST payloads

### External Integrations
- RabbitMQ (AMQP)
  - Default: `localhost:5672` (guest/guest)
  - Event externalization target via `@Externalized("exchange::routingKey")`
  - Inbound queue: `new-orders` (listener consumes and publishes `OrderCreatedEvent`)
- HTTP/REST for `OrderController`

### Monitoring & Logs
- SLF4J logging for inbound message payloads and event processing
- Event Publication Registry tables for operational visibility

## Development Environment

### Build & Development Tools
- Build System: Maven (`./mvnw clean verify`, `./mvnw spring-boot:run`)
- Packaging: `java -jar target/modulithdemo-0.0.1-SNAPSHOT.jar`
- Local URL: `http://localhost:8081`

### Code Quality Tools
- Style: Follow repository conventions (Java 21, 2-space indentation, ~120 columns)
- Testing: JUnit 5 with `@ModulithTest` and `@SpringBootTest` where applicable

### Version Control & Collaboration
- Git repository; follow Conventional Commits
- Focused diffs; maintain module boundaries

## Deployment & Configuration
- Target: JVM (packaged JAR)
- Port: 8081 (configurable)
- RabbitMQ config via Spring Boot properties / env vars:
  - `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`
- Modulith events config:
  - `spring.modulith.events.republish-outstanding-events-on-restart=true` (optional)
  - `spring.modulith.events.completion-mode=UPDATE|DELETE|ARCHIVE`

## Technical Requirements & Constraints

### Performance Requirements
- Moderate throughput; inbound listener handles messages individually
- Asynchronous, separate transactions for `@ApplicationModuleListener` consumers

### Compatibility Requirements
- Java 21
- Spring Boot 3.5.x / Spring Modulith 1.4.x
- RabbitMQ 3.x (AMQP 0-9-1)

## Architectural Decisions (key)
1. Event-first integration using Spring Modulith application events; no cross-module service calls
2. AMQP externalization with `spring-modulith-events-amqp` and `@Externalized`
3. Inbound AMQP adapter with `@RabbitListener` translating transport to `OrderCreatedEvent`
4. Constructor injection; avoid field injection
5. SLF4J for logging; avoid logging secrets
6. Keep AMQP concerns inside `inbound/amqp`; domain remains transport-agnostic

## Example Config Snippets

```yaml
spring:
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:guest}
    password: ${SPRING_RABBITMQ_PASSWORD:guest}

  modulith:
    events:
      republish-outstanding-events-on-restart: true
      completion-mode: UPDATE
```

```java
// Externalization example
@Externalized("domain.events::order.completed")
public record OrderCompleted(UUID orderId) {}

// Inbound adapter example
@RabbitListener(queues = "new-orders")
void onMessage(String payload) {
  log.info("Received new-orders payload: {}", payload);
  events.publishEvent(new OrderCreatedEvent(/* parse payload */));
}
```
