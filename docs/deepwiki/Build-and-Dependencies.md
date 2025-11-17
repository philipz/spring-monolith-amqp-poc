# Build and Dependencies

> **Relevant source files**
> * [mvnw](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/mvnw)
> * [mvnw.cmd](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/mvnw.cmd)
> * [pom.xml](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml)

This document details the Maven build configuration, dependency structure, and build artifacts for the spring-monolith-amqp-poc application. It covers the project's dependency tree, version management, Maven wrapper usage, and Docker image creation.

For information about running the application locally, see [Local Development Setup](/philipz/spring-monolith-amqp-poc/8.3-local-development-setup). For details on the testing framework and test execution, see [Testing Strategy](/philipz/spring-monolith-amqp-poc/8.2-testing-strategy).

---

## Maven Build System

The project uses **Maven 3.x** as its build tool with the **Maven Wrapper** (`mvnw`/`mvnw.cmd`) included for consistent builds across environments. The wrapper eliminates the need for Maven installation and ensures all developers use the same Maven version.

### Maven Wrapper Scripts

The repository includes platform-specific Maven wrapper executables:

| File | Purpose | Platform |
| --- | --- | --- |
| `mvnw` | Maven wrapper shell script | Unix/Linux/macOS |
| `mvnw.cmd` | Maven wrapper batch/PowerShell script | Windows |
| `.mvn/wrapper/maven-wrapper.properties` | Maven distribution configuration | All platforms |

The wrapper automatically downloads and caches the specified Maven distribution on first use. Developers can run builds using `./mvnw` (Unix) or `mvnw.cmd` (Windows) without installing Maven globally.

**Sources:** [mvnw L1-L296](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/mvnw#L1-L296)

 [mvnw.cmd L1-L190](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/mvnw.cmd#L1-L190)

---

## Project Coordinates and Parent POM

The project inherits from `spring-boot-starter-parent` to leverage Spring Boot's dependency management, plugin configuration, and build conventions:

```html
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.5</version>
</parent>

<groupId>com.example</groupId>
<artifactId>amqp-modulith</artifactId>
<version>0.0.1-SNAPSHOT</version>
<name>amqp-modulith</name>
```

**Key Properties:**

* **Java Version:** 21
* **Spring Modulith Version:** 1.4.3
* **Docker Image Name:** `philipz/amqp-modulith:0.0.1-SNAPSHOT`

**Sources:** [pom.xml L5-L33](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L5-L33)

---

## Dependency Architecture

```mermaid
flowchart TD

APP["amqp-modulith<br>Application JAR"]
SB_WEB["spring-boot-starter-web"]
SB_ACT["spring-boot-starter-actuator"]
SB_VAL["spring-boot-starter-validation"]
SB_JDBC["spring-boot-starter-jdbc<br>HikariCP"]
SM_CORE["spring-modulith-starter-core"]
SM_API["spring-modulith-events-api"]
SM_AMQP["spring-modulith-events-amqp"]
SM_JACKSON["spring-modulith-events-jackson"]
SM_JDBC["spring-modulith-starter-jdbc<br>EventPublicationRegistry"]
RABBIT["spring-rabbit<br>RabbitTemplate<br>RabbitListener"]
PG["postgresql<br>Runtime"]
H2["h2<br>Test Only"]
LOMBOK["lombok<br>Optional"]
SB_TEST["spring-boot-starter-test"]
SM_TEST["spring-modulith-starter-test"]

APP --> SB_WEB
APP --> SB_ACT
APP --> SB_VAL
APP --> SB_JDBC
APP --> SM_CORE
APP --> SM_API
APP --> SM_AMQP
APP --> SM_JACKSON
APP --> SM_JDBC
APP --> RABBIT
APP --> PG
APP --> LOMBOK
SM_AMQP --> RABBIT
SM_JDBC --> SB_JDBC
SB_JDBC --> PG
SB_TEST --> APP
SM_TEST --> APP
H2 --> APP

subgraph Testing ["Testing"]
    SB_TEST
    SM_TEST
end

subgraph Utilities ["Utilities"]
    LOMBOK
end

subgraph subGraph4 ["Database Drivers"]
    PG
    H2
end

subgraph Messaging ["Messaging"]
    RABBIT
end

subgraph subGraph2 ["Spring Modulith 1.4.3"]
    SM_CORE
    SM_API
    SM_AMQP
    SM_JACKSON
    SM_JDBC
end

subgraph subGraph1 ["Spring Boot 3.5.5"]
    SB_WEB
    SB_ACT
    SB_VAL
    SB_JDBC
end

subgraph subGraph0 ["Application Runtime"]
    APP
end
```

**Sources:** [pom.xml L34-L142](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L34-L142)

---

## Core Dependencies

### Spring Boot Starters

The application uses the following Spring Boot starters for core functionality:

| Dependency | Purpose | Key Components |
| --- | --- | --- |
| `spring-boot-starter-web` | REST API support | `DispatcherServlet`, `Jackson`, embedded Tomcat |
| `spring-boot-starter-actuator` | Health checks and monitoring | `/actuator` endpoints |
| `spring-boot-starter-validation` | Bean validation | JSR-380 validation annotations |
| `spring-boot-starter-jdbc` | JDBC and connection pooling | `DataSource`, `JdbcTemplate`, HikariCP |

**Sources:** [pom.xml L41-L52](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L41-L52)

 [pom.xml L94-L97](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L94-L97)

### Spring Modulith Dependencies

Spring Modulith provides the event-driven modular architecture foundation:

| Dependency | Artifact ID | Purpose |
| --- | --- | --- |
| Core | `spring-modulith-starter-core` | Module boundary enforcement, event bus |
| Events API | `spring-modulith-events-api` | `@Externalized`, `ApplicationModuleListener` annotations |
| AMQP Integration | `spring-modulith-events-amqp` | Event externalization to RabbitMQ |
| JSON Serialization | `spring-modulith-events-jackson` | Jackson-based event serialization for AMQP |
| JDBC Registry | `spring-modulith-starter-jdbc` | `event_publication` table, transactional outbox |

The Spring Modulith BOM manages version consistency:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>1.4.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Sources:** [pom.xml L36-L38](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L36-L38)

 [pom.xml L54-L73](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L54-L73)

 [pom.xml L143-L153](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L143-L153)

### AMQP/RabbitMQ Integration

```xml
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit</artifactId>
</dependency>
```

The `spring-rabbit` dependency provides:

* `RabbitTemplate` for message publishing (used by `spring-modulith-events-amqp`)
* `@RabbitListener` for message consumption (used in `InboundNewOrderListener`)
* `RabbitAdmin` for topology configuration (used in `NewOrderTopologyConfig`)
* Connection pooling and channel management

**Sources:** [pom.xml L76-L79](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L76-L79)

### Database Dependencies

The application supports multiple database configurations:

| Dependency | Scope | Purpose |
| --- | --- | --- |
| `postgresql` | Runtime | Production Event Publication Registry and application data |
| `h2` | Test | In-memory database for integration tests |

Both drivers integrate with HikariCP connection pooling provided by `spring-boot-starter-jdbc`.

**Sources:** [pom.xml L82-L92](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L82-L92)

---

## Test Dependencies

The project includes comprehensive testing support with explicit Mockito exclusions:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
        </exclusion>
        <!-- Additional Mockito exclusions -->
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <!-- Same Mockito exclusions -->
    </exclusions>
</dependency>
```

**Test Frameworks Included:**

* JUnit 5 (Jupiter)
* AssertJ for fluent assertions
* Spring Test (`@SpringBootTest`, `@WebMvcTest`)
* Spring Modulith test utilities for module verification

**Note:** Mockito is explicitly excluded from both test starters, suggesting the project may use alternative mocking strategies or prefer integration testing over unit testing with mocks.

**Sources:** [pom.xml L98-L135](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L98-L135)

---

## Build Configuration and Plugins

### Spring Boot Maven Plugin

The `spring-boot-maven-plugin` handles application packaging and Docker image creation:

```html
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>philipz/amqp-modulith:0.0.1-SNAPSHOT</name>
        </image>
    </configuration>
</plugin>
```

**Plugin Capabilities:**

* `mvn spring-boot:run` - Run application locally
* `mvn package` - Create executable JAR with embedded dependencies
* `mvn spring-boot:build-image` - Create OCI container image using Buildpacks

The Docker image name follows the pattern: `philipz/${project.artifactId}:${project.version}`

**Sources:** [pom.xml L155-L167](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L155-L167)

### Lombok Configuration

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

Lombok reduces boilerplate with annotations like `@Data`, `@Slf4j`, and `@RequiredArgsConstructor`. The `optional` flag prevents Lombok from being included in downstream dependencies.

**Sources:** [pom.xml L137-L141](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L137-L141)

---

## Build Artifacts and Lifecycle

```mermaid
flowchart TD

SRC["src/main/java<br>OrderManagement<br>InventoryManagement<br>InboundNewOrderListener"]
RES["src/main/resources<br>application.yml<br>application-test.yml"]
TEST["src/test/java<br>Test classes"]
COMPILE["mvn compile<br>Compile Java sources"]
TEST_COMPILE["mvn test-compile<br>Compile tests"]
TEST_RUN["mvn test<br>Run unit tests"]
PACKAGE["mvn package<br>Create JAR"]
TARGET["target/"]
JAR["amqp-modulith-0.0.1-SNAPSHOT.jar<br>Executable JAR"]
CLASSES["target/classes<br>Compiled bytecode"]
BUILD_IMAGE["mvn spring-boot:build-image"]
DOCKER["philipz/amqp-modulith:0.0.1-SNAPSHOT<br>OCI Container Image"]
RUN["Application Runtime"]

SRC --> COMPILE
RES --> COMPILE
TEST --> TEST_COMPILE
PACKAGE --> JAR
PACKAGE --> CLASSES
JAR --> BUILD_IMAGE
JAR --> RUN
DOCKER --> RUN

subgraph subGraph3 ["Docker Image"]
    BUILD_IMAGE
    DOCKER
    BUILD_IMAGE --> DOCKER
end

subgraph subGraph2 ["Build Artifacts"]
    TARGET
    JAR
    CLASSES
end

subgraph subGraph1 ["Maven Build Phases"]
    COMPILE
    TEST_COMPILE
    TEST_RUN
    PACKAGE
    COMPILE --> TEST_COMPILE
    TEST_COMPILE --> TEST_RUN
    TEST_RUN --> PACKAGE
end

subgraph subGraph0 ["Source Code"]
    SRC
    RES
    TEST
end
```

**Common Build Commands:**

| Command | Purpose |
| --- | --- |
| `./mvnw clean` | Remove `target/` directory |
| `./mvnw compile` | Compile main sources |
| `./mvnw test` | Run tests with H2 database |
| `./mvnw package` | Create executable JAR |
| `./mvnw spring-boot:run` | Run application with embedded Tomcat |
| `./mvnw spring-boot:build-image` | Create Docker image via Buildpacks |
| `./mvnw dependency:tree` | Display dependency tree |

**Sources:** [pom.xml L155-L167](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L155-L167)

---

## Dependency Version Management

```mermaid
flowchart TD

PARENT["spring-boot-starter-parent<br>3.5.5"]
BOM["spring-modulith-bom<br>1.4.3"]
PROPS["pom.xml properties<br>java.version=21"]
SB_GROUP["Spring Boot Dependencies<br>spring-boot-starter-*<br>spring-rabbit<br>postgresql<br>h2"]
SM_GROUP["Spring Modulith Dependencies<br>spring-modulith-*"]
SM_API["spring-modulith-events-api<br>version: 1.4.3"]
SM_AMQP["spring-modulith-events-amqp<br>version: 1.4.3"]
SM_JACKSON["spring-modulith-events-jackson<br>version: 1.4.3"]
SM_JDBC["spring-modulith-starter-jdbc<br>version: 1.4.3"]

PARENT --> SB_GROUP
BOM --> SM_GROUP
PROPS --> SM_API
PROPS --> SM_AMQP
PROPS --> SM_JACKSON
PROPS --> SM_JDBC

subgraph subGraph2 ["Explicit Versions"]
    SM_API
    SM_AMQP
    SM_JACKSON
    SM_JDBC
end

subgraph subGraph1 ["Managed Dependencies"]
    SB_GROUP
    SM_GROUP
end

subgraph subGraph0 ["Version Sources"]
    PARENT
    BOM
    PROPS
end
```

**Version Hierarchy:**

1. **Spring Boot Parent POM** (3.5.5) - Manages most Spring Framework and third-party dependencies
2. **Spring Modulith BOM** (1.4.3) - Manages all Spring Modulith artifacts via `<dependencyManagement>` import
3. **Explicit Versions** - Some Spring Modulith dependencies explicitly declare `${spring-modulith.version}` for clarity

**Key Version Properties:**

| Property | Value | Purpose |
| --- | --- | --- |
| `java.version` | 21 | Target JVM version |
| `spring-modulith.version` | 1.4.3 | Centralized Spring Modulith version |
| `dockerImageName` | `philipz/amqp-modulith:0.0.1-SNAPSHOT` | Container image tag |

**Sources:** [pom.xml L29-L33](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L29-L33)

 [pom.xml L143-L153](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L143-L153)

---

## Dependency Scope Summary

| Scope | Dependencies | Inclusion in Runtime |
| --- | --- | --- |
| `compile` (default) | Spring Boot starters, Spring Modulith, `spring-rabbit`, Lombok (optional) | Included in JAR and classpath |
| `runtime` | `postgresql` | Not needed for compilation, included at runtime |
| `test` | `h2`, `spring-boot-starter-test`, `spring-modulith-starter-test` | Only available during test execution |

The executable JAR produced by `mvn package` includes all `compile` and `runtime` dependencies but excludes `test`-scoped dependencies. The `optional` Lombok dependency is included in compilation but may be excluded from transitive dependency resolution.

**Sources:** [pom.xml L34-L142](https://github.com/philipz/spring-monolith-amqp-poc/blob/c93f55b5/pom.xml#L34-L142)