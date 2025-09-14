# Requirements Document

## Introduction

Implement an inbound AMQP listener that connects to RabbitMQ and listens to queue `new-orders`. Upon receiving a message, the adapter shall:
- Log the received message content using SLF4J at info level.
- Publish an internal Spring Modulith application event `OrderCreatedEvent` for downstream modules to handle via `@ApplicationModuleListener`.

This feature bridges external order creation messages into the application's event-driven modular architecture without introducing cross-module direct dependencies.

## Alignment with Product Vision

- Embraces Spring Modulith's event-first integration between modules, avoiding direct calls across boundaries.
- Uses an inbound adapter in `inbound/amqp` to translate transport messages into domain events, keeping the domain isolated from AMQP concerns.
- Leverages reliability patterns from the Event Publication Registry for downstream processing when consumers use `@ApplicationModuleListener`.

References:
- `Application_Events_and_AMQP_Integration.md`
- `Event-design.md`

## Requirements

### Requirement 1 — RabbitMQ inbound listener (new-orders)

**User Story:** As an integration engineer, I want the application to subscribe to RabbitMQ queue `new-orders`, so that external systems can push newly created orders and our application can react in a modular way.

#### Acceptance Criteria
1. WHEN a message arrives on queue `new-orders` THEN the system SHALL log the full payload content at info level using SLF4J.
2. WHEN a message arrives on queue `new-orders` THEN the system SHALL publish an internal application event `OrderCreatedEvent` containing the parsed payload fields (at minimum the external `orderId`).
3. IF message deserialization fails THEN the system SHALL log an error with context and avoid endless redelivery loops (e.g., reject without requeue or route to DLQ if configured).
4. IF RabbitMQ is unavailable at startup THEN the application SHALL still be startable when AMQP is disabled via profile; the inbound listener SHALL be guarded by an `amqp` profile as needed for development environments.

### Requirement 2 — Internal event publication (OrderCreatedEvent)

**User Story:** As a downstream module developer (e.g., Inventory feature), I want to receive a `OrderCreatedEvent` via Spring Modulith application events, so that I can process follow-up actions asynchronously without coupling to AMQP or the inbound adapter.

#### Acceptance Criteria
1. WHEN the inbound adapter handles a valid `new-orders` message THEN the system SHALL publish a `OrderCreatedEvent` via Spring's `ApplicationEventPublisher`.
2. THEN downstream consumers using `@ApplicationModuleListener` SHALL be able to subscribe and handle `OrderCreatedEvent` in their own transactions.
3. The event SHALL be defined in the domain layer (e.g., `domain/order/events`) and contain minimally `orderId` (UUID); additional fields can be added once the message schema is finalized.

### Requirement 3 — Configuration & topology

**User Story:** As an operator, I want the AMQP connection and queue name to be configurable, so that I can adapt to different environments without code changes.

#### Acceptance Criteria
1. RabbitMQ connection properties SHALL be configurable via `application.yml` and environment variables (e.g., `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, etc.).
2. The listener SHALL consume from queue name `new-orders` by default; the name MAY be overridden via configuration property if needed in the future.
3. Queue/exchange declaration is OPTIONAL in this feature; if topology auto-declaration is provided, it SHALL not break when the queue already exists.

## Non-Functional Requirements

### Code Architecture and Modularity
- Single responsibility: inbound AMQP adapter code only translates AMQP messages to internal events and logging; it MUST NOT contain business logic.
- Modular design: place inbound code under `inbound/amqp`; place event type under `domain/order` package (e.g., `domain/order/events`).
- Clear interfaces: communicate across module boundaries via application events, not direct service calls.

### Performance
- Listener processes messages individually; no batch requirement. Throughput goals are modest and bound by RabbitMQ delivery rate and downstream consumers.

### Security
- Do not commit credentials. Use environment variables for RabbitMQ host/user/password.
- Ensure no sensitive data is logged when printing message payloads; redact if needed.

### Reliability
- Acknowledge semantics should avoid poison-message loops. Prefer rejecting malformed messages without requeue or use a DLQ if present.
- Downstream processing via `@ApplicationModuleListener` benefits from Event Publication Registry guarantees as per `Application_Events_and_AMQP_Integration.md`.

### Usability
- Log informative entries on receive, success, and error paths to aid troubleshooting.

## Open Questions
- External message schema: What exact fields does `new-orders` carry beyond `orderId`? Provide sample JSON to finalize the DTO and `OrderCreatedEvent` fields.
- Error handling policy: Confirm whether to use DLQ, nack-without-requeue, or retry policy for deserialization errors.
- Profile strategy: Should AMQP inbound be always-on or gated behind a dedicated Spring profile (e.g., `amqp`)?

## Out of Scope
- Event externalization from internal domain events to RabbitMQ (already covered by separate mechanism and docs), unless explicitly requested.
- Downstream consumer implementations beyond ensuring `OrderCreatedEvent` is published and log output is present.

## Traceability
- Aligns with guidance in `Application_Events_and_AMQP_Integration.md` and `Event-design.md` regarding:
  - Using application events for cross-module integration (`@ApplicationModuleListener`).
  - Treating inbound AMQP as an adapter translating transport messages to internal events.
  - Configuring RabbitMQ via Spring Boot properties and avoiding hard-coded settings.
