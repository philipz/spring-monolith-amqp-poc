# Spring Modulith Application Events and AMQP Integration

Spring Modulith's Application Events provide a **sophisticated event-driven architecture framework** for building modular Spring Boot applications with robust reliability guarantees through transactional event processing and seamless AMQP integration. The framework enables loose coupling between application modules while maintaining transactional consistency and providing enterprise-grade reliability through the Event Publication Registry.

## Spring Modulith Application Events fundamentals

### Event-driven modular architecture

Spring Modulith promotes **event-driven communication as the primary integration mechanism** between application modules to maintain loose coupling. Instead of direct bean dependencies between modules, the framework encourages event publication through Spring's `ApplicationEventPublisher` and consumption via annotated listener methods within clearly defined transactional boundaries.

The architecture delivers significant benefits including **complete decoupling** where originating modules don't need knowledge of interested parties, enhanced testability through isolated module testing without complex mocking, natural domain alignment where events represent business concepts and state transitions, and improved evolvability allowing new functionality integration without modifying existing modules.

### Event publishing and listening patterns

**Event publication** follows Spring's standard pattern using `ApplicationEventPublisher`:

```java
@Service
@RequiredArgsConstructor
public class OrderManagement {
    private final ApplicationEventPublisher events;
    
    @Transactional
    public void complete(Order order) {
        // State transition on the order aggregate
        events.publishEvent(new OrderCompleted(order.getId()));
    }
}
```

**Event consumption** utilizes the recommended `@ApplicationModuleListener` annotation, which combines `@Async`, `@Transactional(propagation = Propagation.REQUIRES_NEW)`, and `@TransactionalEventListener` to ensure proper transaction isolation and asynchronous processing:

```java
@Component
class InventoryManagement {
    @ApplicationModuleListener
    void on(OrderCompleted event) {
        // Runs asynchronously in separate transaction with REQUIRES_NEW propagation
        updateInventoryForCompletedOrder(event);
    }
}
```

### Cross-module communication and transaction handling

The framework implements sophisticated **transaction handling through the Event Publication Registry (EPR)**, which provides the transactional outbox pattern for reliable event processing. When events are published, the registry identifies transactional event listeners and creates log entries within the original business transaction, ensuring atomic consistency between business state changes and event publication.

**Event processing guarantees** include at-least-once delivery where the Event Publication Registry ensures events aren't lost, transaction isolation with application module listeners running in separate transactions, and automatic republication capabilities through configuration of `spring.modulith.events.republish-outstanding-events-on-restart=true` for automatic retry on application restart.

The **Event Publication Registry database schema** automatically creates the following structure:

```sql
CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
```

### Event lifecycle and completion modes

Spring Modulith 1.3+ supports **three completion modes** via `spring.modulith.events.completion-mode`:

- **UPDATE** (default): Sets completion date while keeping records for inspection
- **DELETE**: Removes completed events immediately, reducing storage requirements  
- **ARCHIVE**: Copies to archive table/collection, then removes original

The **Event Publication Management APIs** provide powerful capabilities for production environments:

```java
@Autowired
private CompletedEventPublications completedEvents;
@Autowired  
private IncompleteEventPublications incompleteEvents;

// Purge events older than 1 hour
completedEvents.deleteOlderThan(Duration.ofHours(1));

// Resubmit failed events
incompleteEvents.resubmit(Duration.ofMinutes(5));
```

## spring-modulith-events-amqp integration with RabbitMQ

### Configuration and setup

The **core dependency** for AMQP integration requires:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-amqp</artifactId>
    <version>1.4.3</version>
</dependency>

<!-- Event Publication Registry -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>
```

**BOM configuration** is recommended for consistent versioning:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>1.4.3</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Event externalization architecture

The library **bridges Spring Application Events with RabbitMQ** through a sophisticated three-step process: event selection where events annotated with `@Externalized` are automatically selected for externalization, message preparation where events are serialized using Jackson JSON with optional custom headers, and routing target determination where messages are routed to AMQP exchanges based on configuration.

**Event externalization examples** demonstrate the flexibility:

```java
// Basic externalization to exchange
@Externalized("order.completed")
public record OrderCompleted(String orderId, String customerId) {}

// Advanced routing with dynamic keys
@Externalized("customer-events::#{#this.getLastname()}")
public record CustomerCreated(String customerId, String lastname) {
    public String getLastname() { return lastname; }
}
```

### Queue subscription and message handling

**AMQP-specific routing** follows the pattern:
- Simple: `"exchange.name"`
- With routing key: `"exchange.name::routing.key"`
- Dynamic routing key: `"exchange.name::#{#this.propertyName()}"`

**Programmatic configuration** enables advanced scenarios:

```java
@Configuration
class ExternalizationConfiguration {
    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
            .select(EventExternalizationConfiguration.annotatedAsExternalized())
            .headers(event -> Map.of("custom-header", "value"))
            .routeKey(WithKeyProperty.class, WithKeyProperty::getKey)
            .build();
    }
}
```

### Integration with existing RabbitMQ infrastructure

**Standard RabbitMQ configuration** works seamlessly with Spring Boot:

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:secret}
    connection-timeout: 60000
```

**Production-ready configuration** includes comprehensive event management:

```yaml
spring:
  modulith:
    events:
      republish-outstanding-events-on-restart: true
      completion-mode: ARCHIVE
      jdbc-schema-initialization:
        enabled: true
```

### Message serialization and format considerations

**Default serialization** uses JSON via Jackson `ObjectMapper` with automatic Spring Boot auto-configuration. **Custom serialization** is supported through implementing the `EventSerializer` interface:

```java
@Component
public class CustomEventSerializer implements EventSerializer {
    @Override
    public String serialize(Object event) {
        // Custom serialization logic
    }
}
```

Published messages contain the serialized event object as payload, standard AMQP headers plus custom headers, and exchange and routing key based on `@Externalized` configuration.

## Practical code examples and GitHub projects

### Real-world implementations

**Official Spring Modulith Examples** provide the foundational reference implementation in the `spring-projects/spring-modulith` repository with official examples demonstrating event externalization to various brokers including AMQP, comprehensive reference documentation, and complete `spring-modulith-events-amqp` module for RabbitMQ integration.

**Production-ready examples** include the `antonioalmeida/spring-modulith-externalization-demo` focusing on testing success and failure scenarios with PostgreSQL integration and event publication registry, SQS queue externalization examples, and comprehensive error handling and retry mechanism testing with three endpoints demonstrating different scenarios.

The **ZenWave360 Spring Cloud Stream integration** (`ZenWave360/spring-modulith-events-spring-cloud-stream`) offers innovative event externalization using Spring Cloud Stream with JSON and Avro serialization support, dynamic routing based on channel binder type, and support for multiple brokers including RabbitMQ and Kafka.

### Complete working examples

**Event publishing implementation**:

```java
@Service
@Transactional
public class OrderService {
    private final ApplicationEventPublisher events;
    
    public Order completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId);
        order.complete();
        
        // Event published within transaction
        events.publishEvent(new OrderCompleted(order.getId(), order.getCustomerId()));
        return orderRepository.save(order);
    }
}
```

**Event consumption with error handling**:

```java
@Component
public class InventoryEventHandler {
    
    @ApplicationModuleListener
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleOrderCompleted(OrderCompleted event) {
        try {
            inventoryService.updateInventory(event.orderId());
            log.info("Successfully processed order completion: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process order completion: {}", event.orderId(), e);
            throw e; // Will trigger retry mechanism
        }
    }
    
    @Recover
    public void handleOrderCompletedFailure(Exception ex, OrderCompleted event) {
        log.error("All retry attempts exhausted for order: {}", event.orderId());
        // Handle ultimate failure scenario
    }
}
```

### Error handling and retry mechanisms

**Current challenges** in the ecosystem include Issue #922 where Event Publication Registry marks entries as completed even when AMQP publication fails due to non-existing exchanges, Issue #438 where DelegatingEventExternalizer fails when delegate returns null, and Issue #454 regarding event externalization with AMQP completion failures.

**Recommended solutions** include proper Event Publication Registry configuration and manual event management:

```java
@Scheduled(fixedDelay = 300000) // Every 5 minutes
public void retryIncompleteEvents() {
    incompleteEvents.resubmitOlderThan(Duration.ofMinutes(5));
    log.info("Resubmitted incomplete events older than 5 minutes");
}

@Scheduled(fixedDelay = 3600000) // Every hour  
public void cleanupCompletedEvents() {
    completedEvents.deletePublicationsOlderThan(Duration.ofDays(1));
    log.info("Cleaned up completed events older than 1 day");
}
```

### Integration testing patterns

**Scenario DSL testing** provides comprehensive test capabilities:

```java
@ApplicationModuleTest
@EnableScenarios
class OrderIntegrationTests {
    @Test
    void publishesOrderCompletion(Scenario scenario) {
        scenario.stimulate(() -> orderService.completeOrder(1L))
                .andWaitForStateChange(registry::findIncompletePublications, Collection::isEmpty)
                .andExpected(OrderCompleted.class)
                .matchingMappedValue(OrderCompleted::orderId, 1L)
                .toArriveAndAssert((event, result) -> 
                    assertThat(inventoryService.getStock()).isLessThan(100));
    }
}
```

## Architecture patterns and enterprise implementations

### Event sourcing with Spring Modulith

**Event sourcing implementations** leverage the Event Publication Registry as a foundation for the transactional outbox pattern, ensuring eventual consistency across systems. The EPR hooks into Spring Framework's core event publication mechanism, creating registry entries for transactional event listeners as part of the original business transaction.

**Database technology support** includes PostgreSQL recommended for applications requiring familiarity with relational databases, EventStore for performance and scalability scenarios, and MongoDB/Neo4j through dedicated starters with full transaction support.

**Complete event sourcing implementation**:

```java
@Entity
public class EventEntity {
    @Id
    private UUID id;
    private LocalDateTime timestamp;
    private String eventType;
    private String eventData;
    private String aggregateId;
    private Long sequenceNumber;
}

@Service
public class EventSourcingService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void replayEventsForAggregate(String aggregateId) {
        List<EventEntity> events = eventRepository
            .findByAggregateIdOrderBySequenceNumber(aggregateId);
            
        events.forEach(entity -> {
            Object event = deserializeEvent(entity);
            eventPublisher.publishEvent(event);
        });
    }
}
```

### CQRS implementation using events

**Advanced CQRS architecture** with Spring Modulith implements clear separation between command and query responsibilities through modular boundaries. The command module handles write operations with domain aggregates, the query module maintains optimized read models with separate database schemas, and the event bridge provides asynchronous communication via Spring Modulith events.

**Transaction isolation benefits** ensure that query-side processing runs independently:

```java
@ApplicationModuleListener
public void on(ProductCreatedEvent event) {
    // Runs in separate transaction from command side
    ProductView view = ProductView.builder()
        .id(event.productId())
        .name(event.name())
        .price(event.price())
        .build();
    viewRepository.save(view);
}
```

**Enterprise advantages** include clear separation of concerns with enforced module boundaries, independent scalability for read and write operations, resilience where command-side issues don't impact query operations, and automatic event auditing with retry capabilities for failed events.

### Module-to-module communication patterns

**Event-driven orchestration** supports both service choreography and orchestration approaches with AMQP routing patterns using Exchange → Routing Key → Event Queue patterns and integration capabilities with BPMN engines for complex business processes.

**Module interaction design patterns** balance direct dependency injection versus event-based communication, considering trade-offs in testability, consistency, and error handling while enforcing module boundaries through package conventions and compilation checks.

**Advanced communication architecture**:

```java
// Order Module - Command Side
@Service
public class OrderService {
    @Transactional
    public void processPayment(PaymentRequest request) {
        // Process payment logic
        events.publishEvent(new PaymentProcessed(request.orderId(), request.amount()));
    }
}

// Inventory Module - Event Handler
@Component
public class InventoryEventHandler {
    @ApplicationModuleListener
    public void on(PaymentProcessed event) {
        // Reserve inventory in separate transaction
        inventoryService.reserveItems(event.orderId());
    }
}

// Notification Module - Event Handler  
@Component
public class NotificationEventHandler {
    @ApplicationModuleListener
    @Externalized("customer-notifications::#{customerId}")
    public void on(PaymentProcessed event) {
        // Send notification and externalize to AMQP
        notificationService.sendPaymentConfirmation(event);
    }
}
```

### Scalability and performance considerations

**Performance optimization techniques** in Spring Modulith 1.4+ include significant improvements in core event publication registry performance, enhanced observability instrumentation, and flexible completion modes (UPDATE, DELETE, ARCHIVE) for optimal event lifecycle management.

**Micrometer integration** provides automatic metrics collection:

```java
@Bean
ModulithEventMetricsCustomizer metricsCustomizer() {
    return customizer -> customizer.tag("module", "inventory");
}
```

**Enterprise scalability patterns** enable horizontal module scaling where individual modules can be independently scaled, strategic event partitioning through routing keys for load distribution, optimized AMQP connection pooling for high throughput, and event batching capabilities for improved performance.

**Production observability configuration**:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: modulith
spring:
  modulith:
    observability:
      enabled: true
```

### Microservices evolution patterns

**Evolutionary architecture approach** recommends starting with modular monolith using Spring Modulith, identifying extraction candidates through module metrics and usage patterns, and implementing gradual transformation to microservices when justified by business requirements rather than technical preferences.

**Event externalization infrastructure** supports multiple brokers through dedicated artifacts: `spring-modulith-events-amqp` for RabbitMQ with routing key support, `spring-modulith-events-kafka` for Kafka with topic and message key routing, and `spring-modulith-events-jms` for standard JMS integration patterns.

**Migration strategy implementation**:

```java
// Modular monolith phase - internal events
@ApplicationModuleListener
void handleOrderEvent(OrderCompleted event) {
    // Internal processing
}

// Hybrid phase - selective externalization  
@Externalized("orders.completed::#{customerId}")
@ApplicationModuleListener  
void handleOrderEvent(OrderCompleted event) {
    // Both internal processing and external publishing
}

// Microservices phase - full externalization
@Externalized("orders.completed::#{customerId}")
public record OrderCompleted(String orderId, String customerId) {}
```

**Best practices from production usage** emphasize domain-driven design integration with package structure aligned to bounded contexts, module boundaries enforced through compilation checks, named interfaces for controlled inter-module exposure, and comprehensive performance benefits including significant reduction in network latency compared to microservices, simplified debugging and troubleshooting, lower infrastructure costs and operational complexity, and faster development iteration cycles.

## Conclusion

Spring Modulith Application Events with AMQP integration represents a mature, production-ready approach to enterprise architecture that provides a compelling middle ground between monolithic and microservice architectures. The framework's sophisticated event-driven communication patterns, combined with robust AMQP integration capabilities, deliver enterprise-grade reliability through the Event Publication Registry while maintaining architectural flexibility for future evolution.

**Key success factors** for implementation include starting with domain-driven module boundaries aligned to business capabilities, leveraging event-driven communication patterns for loose coupling while maintaining transactional consistency, implementing comprehensive testing strategies using the Scenario DSL and ApplicationModuleTest, planning for gradual evolution to microservices when justified by business requirements, and focusing on observability and monitoring from the start with built-in Micrometer integration and event publication tracking.

The extensive ecosystem of tutorials, real-world examples, and production implementations demonstrates strong industry adoption with compelling benefits including reduced operational complexity, improved development velocity, and simplified debugging while maintaining the architectural flexibility to evolve toward microservices when business scale and team structures justify the additional complexity.