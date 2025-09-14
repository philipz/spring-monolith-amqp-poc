# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5 application using Spring Modulith 1.4.3 that demonstrates modular architecture with event-driven communication between modules. The project uses Java 21 and Maven for build management.

## Build and Run Commands

### Build & Test
```bash
# Full build with tests
./mvnw clean verify

# Run tests only
./mvnw test

# Run a single test class
./mvnw test -Dtest=OrderCreatedFlowTests

# Run tests matching a pattern
./mvnw test -Dtest=*ListenerTests
```

### Run Application
```bash
# Run locally (default port 8081, AMQP enabled by default)
./mvnw spring-boot:run

# Build and run JAR
./mvnw clean package
java -jar target/modulithdemo-0.0.1-SNAPSHOT.jar

# Run without RabbitMQ connection attempts
java -jar target/modulithdemo-0.0.1-SNAPSHOT.jar \
  --spring.modulith.events.externalization.enabled=false \
  --spring.rabbitmq.listener.simple.auto-startup=false
```

## Architecture

### Module Structure

The application follows Spring Modulith conventions with module boundaries defined by package structure:

- **`domain.order`**: Core order management domain
  - `OrderManagement`: Service that publishes domain events
  - `OrderCompleted`: Domain event externalized to AMQP (`domain.events::order.completed`)
  - `OrderCreatedEvent`: Domain event externalized to AMQP (`BookStoreExchange::orders.new`)
  - `OrderController`: REST endpoint for order operations

- **`feature.inventory`**: Inventory management feature module
  - `InventoryManagement`: Reacts to order events using `@ApplicationModuleListener`

- **`inbound.amqp`**: AMQP integration module (active by default)
  - `RabbitTopologyConfig`: Defines exchanges, queues, and bindings
  - `InboundAmqpAdapter`: Receives messages from RabbitMQ and publishes internal events
  - `InboundNewOrderListener`: Converts incoming JSON messages to domain events

### Event Flow Architecture

1. **Internal Events**: Published via `ApplicationEventPublisher`, consumed by `@ApplicationModuleListener`
2. **Event Externalization**: Automatic AMQP publishing via `@Externalized` annotations
3. **Event Publication Registry**: Transactional outbox pattern using H2 database for reliability
4. **Inbound AMQP**: Listeners that convert external messages to internal events (active by default)

### Key Spring Modulith Patterns

- **Module Communication**: Only through events, never direct bean dependencies
- **Transaction Boundaries**: `@ApplicationModuleListener` creates new transactions with `REQUIRES_NEW`
- **Event Reliability**: Event Publication Registry ensures at-least-once delivery
- **AMQP Integration**: Seamless event externalization with routing key patterns

## Testing Approach

- **Unit Tests**: Test individual components in isolation
- **`@ModulithTest`**: Test module boundaries and event flows
- **`@SpringBootTest`**: Full application context tests
- Test classes should end with `*Tests.java` and be placed in corresponding test packages

## Configuration

Key configuration in `application.yml`:
- Server port: 8081
- RabbitMQ: localhost:5672 (guest/guest)
- H2 in-memory database for Event Publication Registry
- Event externalization enabled by default

Override via environment variables:
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`
- `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`

## Development Guidelines

1. **Event-Driven Design**: Prefer events for cross-module communication
2. **Module Boundaries**: Keep modules loosely coupled through events
3. **Transaction Management**: Use `@ApplicationModuleListener` for proper transaction isolation
4. **Testing**: Write tests for event publishing and consumption flows
5. **AMQP Integration**: Use `@Externalized` for automatic event externalization