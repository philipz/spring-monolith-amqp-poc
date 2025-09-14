# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/java/com/example/modulithdemo`
  - `domain/order`: Core domain + REST (`OrderController`, `OrderManagement`, events like `OrderCompleted`).
  - `feature/inventory`: Features reacting to domain events (listeners).
  - `inbound/amqp`: Adapters (RabbitMQ topology + listener).
- Config: `src/main/resources/application.yml` (port 8081, H2, RabbitMQ, Modulith event externalization).
- Tests: `src/test/java` mirroring main package layout.

## Build, Test, and Development Commands
- `./mvnw clean verify` — Build and run tests.
- `./mvnw spring-boot:run` — Start locally at `http://localhost:8081`.
- `java -jar target/modulithdemo-0.0.1-SNAPSHOT.jar` — Run packaged JAR.
- Example: `curl -X POST http://localhost:8081/orders/<UUID>/complete` to publish an order-completed event.

## Coding Style & Naming Conventions
- Java 21. Indentation: 2 spaces; keep lines readable (~120 chars).
- Packages lower-case; classes `PascalCase`; methods/fields `camelCase`.
- Prefer constructor injection; avoid field injection.
- Logging: prefer SLF4J (`LoggerFactory.getLogger(...)`) for new code.
- Modulith boundaries: interact across modules via events, not direct calls.

## Testing Guidelines
- Frameworks: JUnit 5 (`spring-boot-starter-test`) + `spring-modulith-starter-test`.
- Place tests under `src/test/java` with matching package and name them `*Tests.java` (e.g., `OrderManagementTests`).
- Use `@ModulithTest` for module-level integration and `@SpringBootTest` for app-level tests.
- Run: `./mvnw test` (CI-critical); add tests for new endpoints, listeners, and event flows.

## Commit & Pull Request Guidelines
- Use Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
- PRs must include: concise description, linked issues, test coverage for changes, and notes on configuration impacts (RabbitMQ, ports, DB).
- Keep diffs focused; follow module/package boundaries and update docs if event contracts change.

## Security & Configuration Tips
- Defaults: RabbitMQ at `localhost:5672` (guest/guest), H2 in-memory. Override via env (e.g., `SPRING_RABBITMQ_HOST`) or CLI (`--spring.rabbitmq.host=...`).
- Avoid committing secrets. Prefer environment variables or externalized config.
- If RabbitMQ isn’t available locally, develop features that don’t require AMQP, or guard listeners by profile.

