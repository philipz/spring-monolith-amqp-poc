# Tasks Document

- [x] 1. Add inbound DTO NewOrderMessage
  - File: src/main/java/com/example/modulithdemo/inbound/amqp/NewOrderMessage.java
  - Purpose: Create Jackson-friendly DTO for queue `new-orders` with fields orderNumber, productCode, quantity, and nested customer {name,email,phone}.
  - _Leverage: src/main/java/com/example/modulithdemo/domain/order/Customer.java_
  - _Requirements: R1.2, R2.1-R2.3_
  - _Prompt: Implement the task for spec amqp-new-order-listener, first run spec-workflow-guide to get the workflow guide then implement the task: | Role: Spring Boot Java developer with AMQP and Modulith experience | Task: Define record NewOrderMessage with nested NewOrderCustomer in inbound/amqp; tolerate unknown JSON fields | Restrictions: No business logic; do not replace domain types; prefer records; minimal annotations | Success: Compiles; binds JSON payloads; no domain coupling; mark [-] when starting and [x] when done_

- [x] 2. Implement InboundNewOrderListener
  - File: src/main/java/com/example/modulithdemo/inbound/amqp/InboundNewOrderListener.java
  - Purpose: Listen to queue `new-orders`, log payload, publish OrderCreatedEvent to internal event bus.
  - _Leverage: ApplicationEventPublisher; src/main/java/com/example/modulithdemo/domain/order/OrderCreatedEvent.java; SLF4J_
  - _Requirements: R1.1-R1.2, R2.1-R2.2_
  - _Prompt: Implement the task for spec amqp-new-order-listener, first run spec-workflow-guide to get the workflow guide then implement the task: | Role: Messaging adapter developer | Task: Create @RabbitListener(queues="new-orders") method receiving String or NewOrderMessage, log at info level, map fields to OrderCreatedEvent and publish | Restrictions: No domain logic; no cross-module calls; constructor injection; use SLF4J | Success: On message, logs payload and emits OrderCreatedEvent; compile succeeds; mark [-] then [x]_

- [x] 3. Declare RabbitMQ topology for new-orders
  - File: src/main/java/com/example/modulithdemo/inbound/amqp/NewOrderTopologyConfig.java
  - Purpose: Idempotently declare durable queue `new-orders`; optionally bind to exchange `BookStoreExchange` with routing key `orders.new`.
  - _Leverage: Existing RabbitTopologyConfig patterns_
  - _Requirements: R3.1-R3.3_
  - _Prompt: Implement the task for spec amqp-new-order-listener, first run spec-workflow-guide to get the workflow guide then implement the task: | Role: Spring AMQP engineer | Task: Provide @Configuration beans for queue and optional binding (no breaking changes) | Restrictions: Do not remove/alter existing RabbitTopologyConfig; keep names stable; allow operator-managed topology | Success: Context starts with broker; queue exists or is declared safely; mark [-] then [x]_

- [x] 4. Guard inbound beans with profile amqp
  - File: src/main/java/com/example/modulithdemo/inbound/amqp/InboundNewOrderListener.java
  - File: src/main/java/com/example/modulithdemo/inbound/amqp/NewOrderTopologyConfig.java
  - Purpose: Allow disabling AMQP inbound in dev by not activating `amqp` profile.
  - _Leverage: Spring @Profile_
  - _Requirements: R1.4_
  - _Prompt: Implement the task for spec amqp-new-order-listener, first run spec-workflow-guide to get the workflow guide then implement the task: | Role: Spring Boot profile/config specialist | Task: Annotate inbound listener and topology config with @Profile("amqp") | Restrictions: Do not change global defaults; minimal footprint | Success: Beans only load with amqp profile; mark [-] then [x]_

- [x] 5. Unit tests for listener
  - File: src/test/java/com/example/modulithdemo/inbound/amqp/InboundNewOrderListenerTests.java
  - Purpose: Validate mapping and logging (happy path + malformed payload) without real broker.
  - _Leverage: spring-boot-starter-test_
  - _Requirements: R1.1-R1.3, R2.1_
  - _Prompt: Implement the task for spec amqp-new-order-listener, first run spec-workflow-guide to get the workflow guide then implement the task: | Role: Java test engineer (JUnit 5, Mockito) | Task: Mock ApplicationEventPublisher; assert publishEvent(OrderCreatedEvent) with mapped fields; malformed payload logs and does not throw | Restrictions: No Spring context if not needed; no real RabbitMQ | Success: Tests green via ./mvnw test; mark [-] then [x]_

- [x] 6. Modulith test for event flow
  - File: src/test/java/com/example/modulithdemo/feature/inventory/OrderCreatedFlowTests.java
  - Purpose: Verify `OrderCreatedEvent` is consumable by module listeners using @ModulithTest.
  - _Leverage: spring-modulith-starter-test; existing InventoryManagement logging_
  - _Requirements: R2.2_
  - _Prompt: Implement the task for spec amqp-new-order-listener, first run spec-workflow-guide to get the workflow guide then implement the task: | Role: Modulith integration tester | Task: Publish OrderCreatedEvent and assert listener handles it (e.g., via log/assert) | Restrictions: No live broker; keep self-contained | Success: Test passes in CI; mark [-] then [x]_
