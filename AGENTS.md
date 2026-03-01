# AGENTS.md - Developer Guide for Search Project

## Project Overview

This is a Spring Boot 4.0.2 application with Java 25 that provides search functionality using OpenSearch.

## Build Commands

### Gradle Wrapper
The project uses Gradle via the wrapper (`./gradlew` on Unix, `gradlew.bat` on Windows).

### Build Tasks
- **Full build**: `./gradlew clean build` - Compiles, tests, and creates JAR
- **Compile only**: `./gradlew compileJava`
- **Run application**: `./gradlew bootRun`

### Testing
- **Run all tests**: `./gradlew test`
- **Run single test class**: `./gradlew test --tests "SearchApplicationTests"`
- **Run single test method**: `./gradlew test --tests "SearchApplicationTests.contextLoads"`
- **Run tests with verbose output**: `./gradlew test --info`
- **Generate test report**: `./gradlew test` generates HTML reports in `build/reports/tests/test/`

### Other Commands
- **Clean build artifacts**: `./gradlew clean`
- **List available tasks**: `./gradlew tasks`

## Code Style Guidelines

### General Conventions
- Java 25 language level
- Package structure: `org.example.search.{config, controller, dto, model, service}`
- Use Lombok to reduce boilerplate code
- Use constructor injection (preferred over field injection)

### Naming Conventions
- **Classes/Interfaces**: PascalCase (e.g., `OpenSearchService`, `SearchSpec`)
- **Methods/Variables**: camelCase (e.g., `clusterHealth()`, `searchSpec`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `MAX_RESULTS`)
- **Packages**: lowercase with hyphens only where needed

### Imports
- Group imports in this order: java.*, javax.*, org.*, com.*
- Use wildcard imports sparingly (avoid `import java.util.*`)
- Always use fully qualified names for static imports when there's ambiguity

### Lombok Usage
- `@Data`: For DTOs and simple data classes (generates getters, setters, equals, hashCode, toString)
- `@Service`: For service layer classes
- `@RestController`: For REST controllers
- `@Configuration`: For configuration classes
- `@ConfigurationProperties`: For properties classes bound to config
- `@Builder`: Use when builder pattern is needed for complex object construction

### Logging
- Use SLF4J with `LoggerFactory`
- Declare logger as: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
- Log at appropriate levels: ERROR for failures, WARN for warnings, INFO for important events
- Always include exception stack trace in error logs: `logger.error(e.getMessage(), e)`

### Error Handling
- Return null or empty collections for recoverable errors when appropriate
- Throw `RuntimeException` (or custom exceptions) for unrecoverable errors
- Always log exceptions before rethrowing: `logger.error(e.getMessage(), e); throw new RuntimeException(e);`

### REST Controllers
- Use `@RestController` annotation
- Use `@GetMapping`, `@PostMapping`, etc. for HTTP methods
- Include OpenAPI annotations for documentation:
  - `@Operation(summary = "...", description = "...")`
  - `@ApiResponses(value = { @ApiResponse(...) })`
- Constructor injection for dependencies

### Models/DTOs
- Use `@Data` for simple POJOs
- Use `@JsonIgnoreProperties(ignoreUnknown = true)` for OpenSearch document mapping
- Implement `IndexableDocument` interface for documents that can be indexed
- Use Java collection interfaces (List, Map) rather than concrete implementations in public APIs

### Testing
- Use JUnit 5 (`org.junit.jupiter.api.Test`)
- Use `@SpringBootTest` for integration tests
- Test class naming: `ClassNameTests` or `ClassNameIT`
- Test method naming: `methodName_shouldDoX()` or `testMethodName()`

### Configuration
- Use `@ConfigurationProperties` for type-safe configuration binding
- Configuration prefix: lowercase with hyphens (e.g., `open-search.*`)
- Keep configuration in `src/main/resources/application.yaml`

### Additional Patterns
- Use generics properly for type safety
- Use Stream API for collection transformations
- Use builder pattern for complex query objects (OpenSearch client uses builders)
- Prefer immutable objects where possible
- Use `final` for variables that are not reassigned

## Common Tasks

### Adding a New Dependency
1. Add version to `gradle/libs.versions.toml` under `[versions]`
2. Add dependency under `[libraries]` with reference to version
3. Add to `build.gradle` using `libs.*` notation

### Creating a New Service
1. Create class in `src/main/java/org/example/search/service/`
2. Implement interface if multiple implementations exist
3. Use `@Service` annotation
4. Use constructor injection for dependencies

### Creating a New DTO
1. Create class in `src/main/java/org/example/search/dto/`
2. Add `@Data` annotation
3. Add `@JsonIgnoreProperties(ignoreUnknown = true)` if deserializing from JSON

### Creating a New Document Model
1. Create class in `src/main/java/org/example/search/model/`
2. Add `@Data` annotation
3. Add `@JsonIgnoreProperties(ignoreUnknown = true)` if deserializing from JSON
4. Implement `IndexableDocument` interface if it will be stored in OpenSearch
