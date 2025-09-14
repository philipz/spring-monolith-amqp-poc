# Product Overview

## Product Purpose
A Spring Boot (Java 21) Modulith demo showcasing event-driven modular architecture. The app models an Order domain, integrates with RabbitMQ for AMQP messaging, and demonstrates both internal application events and externalization/inbound adapters. It serves as a reference for teams adopting Spring Modulith with clean boundaries between domain, features, and adapters.

## Target Users
- Backend engineers evaluating Spring Modulith for modular monoliths.
- Platform/integration engineers needing AMQP (RabbitMQ) interop.
- Teams building event-first systems with clear module boundaries and testability.

## Key Features
1. Domain module (`domain/order`) with REST endpoints and business services.
2. Internal application events (e.g., `OrderCompleted`) with `@ApplicationModuleListener` consumers.
3. AMQP externalization of selected events via `spring-modulith-events-amqp`.
4. Inbound AMQP adapter (`inbound/amqp`) to translate RabbitMQ messages (e.g., `new-orders`) into internal events (`OrderCreatedEvent`).
5. H2 in-memory DB plus Modulith Event Publication Registry (JDBC) for reliability.
6. Tests using JUnit 5 and `spring-modulith-starter-test` for module-level verification.

## Business Objectives
- Demonstrate decoupled, event-driven module communication without cross-module calls.
- Provide a working AMQP integration pattern (externalization and inbound listener).
- Offer a baseline project that teams can extend for production use.

## Success Metrics
- Build and tests pass via `./mvnw clean verify`.
- App runs locally on `http://localhost:8081` with H2 and RabbitMQ defaults.
- cURL example works and events flow to consumers; inbound `new-orders` messages produce `OrderCreatedEvent` and are logged.
- Clear docs for module boundaries, configuration, and event design.

## Product Principles
1. Event-first integration across modules; no direct cross-module service calls.
2. Adapters at the edges (e.g., AMQP) translate transport <-> domain events.
3. Convention over configuration: predictable packages, naming, and tests.

## Monitoring & Visibility
- SLF4J logging for event publication/consumption and inbound message payloads.
- Event Publication Registry (JDBC) for visibility into processed/pending events.

## Future Vision
### Potential Enhancements
- Dead-letter queues and retry policies for inbound AMQP.
- Schema contracts for events (JSON schema/Avro) and consumer versioning.
- Additional features reacting to domain events (e.g., notifications, billing).
