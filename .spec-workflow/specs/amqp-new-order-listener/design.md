# Design Document

## Overview

Implement an inbound AMQP adapter that listens to RabbitMQ queue `new-orders`, logs the received payload, and publishes an internal Spring Modulith application event `OrderCreatedEvent` for downstream modules to react using `@ApplicationModuleListener`. The adapter lives at the edge (messaging inbound) and contains no business logic.

## Steering Document Alignment

### Technical Standards (tech.md)
- Java 21, Spring Boot 3.5.x, Spring Modulith 1.4.x.
- Use Spring AMQP (`spring-rabbit`) for `@RabbitListener`.
- Use SLF4J for logging; constructor injection; avoid field injection.
- Keep AMQP concerns in the adapter package; domain remains transport-agnostic.

### Project Structure (structure.md)
- Place inbound listener under `messaging/inbound/amqp` (current repo uses `inbound/amqp`; migration guidance provided in structure.md).
- Define `OrderCreatedEvent` in the `order` module (`order/domain/events`).
- Downstream consumers (e.g., `inventory`) subscribe via `@ApplicationModuleListener` in their module’s `app` layer.

## Code Reuse Analysis

### Existing Components to Leverage
- Spring Boot auto-config for RabbitMQ from application.yml.
- Existing Modulith event infrastructure (`spring-modulith-events-api`).

### Integration Points
- RabbitMQ queue: `new-orders` (string payload or JSON DTO; to be finalized).
- Application events: publish `OrderCreatedEvent` via `ApplicationEventPublisher`.

## Architecture

Pattern: Hexagonal/ports-and-adapters. The AMQP listener is an inbound adapter translating external messages to internal events. No business rules in the adapter.

Flow:
1) RabbitMQ delivers message to `@RabbitListener` (queue `new-orders`).
2) Adapter logs payload content at info level.
3) Adapter deserializes payload (if JSON) to a DTO and maps to `OrderCreatedEvent`.
4) Adapter publishes `OrderCreatedEvent` using `ApplicationEventPublisher`.
5) Downstream modules handle the event asynchronously using `@ApplicationModuleListener`.

## Components and Interfaces

### InboundAmqpAdapter (messaging/inbound/amqp)
- Purpose: Subscribe to `new-orders`, log payload, publish internal event.
- Interfaces:
  - Method: `void onMessage(String payload)` or `void onMessage(NewOrderMessage dto)` annotated with `@RabbitListener(queues = "new-orders")`.
- Dependencies: `ApplicationEventPublisher`, Jackson `ObjectMapper` (if using JSON DTO binding), SLF4J logger.
- Reuses: Spring AMQP infrastructure.

### OrderCreatedEvent (order/domain/events)
- Purpose: Internal application event representing that an order was created in an external system.
- Interfaces: Java record/class `OrderCreatedEvent(UUID orderId, …optional fields)`.
- Dependencies: none (domain-owned type).

### Example Consumers (inventory/app)
- Purpose: Demonstrate downstream handling using `@ApplicationModuleListener`.
- Interfaces: `void on(OrderCreatedEvent event)` method.

## Data Models

### OrderCreatedEvent
```
record OrderCreatedEvent(UUID orderId) {}
```

### NewOrderMessage (DTO for inbound, optional)
```
record NewOrderMessage(UUID orderId) {}
```
If the broker sends a raw JSON string, bind method parameter to `String` then parse via `ObjectMapper`.

## Error Handling

### Error Scenarios
1. Malformed payload / JSON parse error
   - Handling: Log error with payload snippet; `nack` without requeue or allow default error handler to route to DLQ if configured.
   - User Impact: Message not reprocessed indefinitely; operations can inspect DLQ.

2. Event publication failure (rare)
   - Handling: Exceptions bubble through listener; log error; rely on container retry/DLQ policy as configured.

3. RabbitMQ unavailable
   - Handling: Allow profile gating (e.g., `@Profile("amqp")`) or conditional bean creation for local dev without RabbitMQ.

## Testing Strategy

### Unit Testing
- InboundAmqpAdapter: invoke `onMessage` with valid/invalid payloads; verify `ApplicationEventPublisher.publishEvent` interactions and logger output.
- Event type immutability and basic contract.

### Integration Testing
- `@ModulithTest` scenario: publish `OrderCreatedEvent` and assert a sample listener reacts.
- Optional `@SpringBootTest` with Testcontainers RabbitMQ (if available) to verify actual listener wiring; otherwise mock listener container.

### End-to-End Testing
- Manual: Send a message to `new-orders` and observe logs plus downstream consumer invocation.

## Implementation Notes
- Keep the adapter stateless and small.
- Prefer `String` payload + explicit `ObjectMapper.readTree()` for resilience until schema is finalized.
- Redact secrets in logs; log only necessary payload fields.
