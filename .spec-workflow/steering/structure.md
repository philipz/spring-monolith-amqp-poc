# Codebase Structure & Conventions

## Directory Layout (proposed)

```
src/
  main/
    java/
      com/example/modulithdemo/
        config/                 # Boot + Modulith config, Rabbit, DB, logging
        shared/                 # Cross-cutting utilities (ids, time, json)
        order/                  # Domain: Order bounded module
          api/                  # REST controllers + DTOs
          domain/               # Aggregates, entities, value objects, events
          app/                  # Application services (use-cases) publishing events
        inventory/              # Feature: reacts to domain events
          app/                  # Event handlers (@ApplicationModuleListener)
        messaging/              # Adapters (anti-corruption layer)
          inbound/
            amqp/               # Rabbit listeners (e.g., new-orders queue)
          outbound/
            amqp/               # Event externalization config or helpers
    resources/
      application.yml           # Port, H2, RabbitMQ, Modulith events config
  test/
    java/
      com/example/modulithdemo/
        order/                  # Unit + Modulith tests for Order module
        inventory/              # Tests for listeners reacting to events
        messaging/              # Tests for inbound/outbound adapters
```

Rationale:
- Package-by-module with clear bounded modules (`order`, `inventory`, `messaging`), not by generic layer, to align with Spring Modulith.
- `messaging` contains all transport concerns (AMQP inbound/outbound) keeping domain pure.
- `shared` holds non-domain-specific helpers; keep it small to avoid coupling sink.
- `config` isolates framework wiring and topology beans from business code.

## Module Boundaries
- Cross-module interactions via application events only; no direct service calls from `inventory` to `order`.
- Domain events named in past tense (e.g., `OrderCompleted`, `OrderCreatedEvent`).
- Inbound AMQP translates transport DTO → internal event; outbound uses `@Externalized` for specific domain events.

## Naming Conventions
- Packages: lowercase; classes: PascalCase; methods/fields: camelCase.
- Events: `{Noun}{PastTense}`; inbound DTOs: `{EventName}Message` or `{EventName}Payload`.
- Config classes end with `Config` (e.g., `RabbitTopologyConfig`).

## Import & Dependency Rules
- `order` depends only on `shared`.
- `inventory` depends on `shared` and events published by `order` (through Spring events, not code deps).
- `messaging` depends on `shared` and can reference event types, but never application services.
- `config` wires infrastructure; avoid importing business packages inside it except for bean exposure.

## Code Structure Patterns
- Inside each module:
  1) `domain` (model + internal events) → 2) `app` (use-cases) → 3) `api` (controllers) or `messaging` adapters.
- Constructor injection, no field injection.
- Log via SLF4J, avoid sensitive data.

## Tests
- Unit tests at module level (`order`, `inventory`, `messaging`).
- Use `@ModulithTest` for event flow verification; `@SpringBootTest` for app-level.
- Mirror source structure; suffix tests with `*Tests`.

## Build & Run
- Build: `./mvnw clean verify`
- Run: `./mvnw spring-boot:run` or packaged JAR

## Migration Guidance (current → proposed)
- Move `inbound/amqp` under `messaging/inbound/amqp`.
- Keep domain events within `order/domain` (or `order/domain/events` if preferred) to emphasize ownership.
- Relocate externalization helpers to `messaging/outbound/amqp`.
- Limit `shared` to small, stable utilities (identifiers, time, serialization helpers).

## Optional Future Split
- If growth requires, evolve to Maven multi-module while keeping same package boundaries:
  - `modulithdemo-order`, `modulithdemo-inventory`, `modulithdemo-messaging`, `modulithdemo-app` (assembly)
- Use Modulith Application Modules to validate boundaries.
