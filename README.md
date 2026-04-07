# Search Service

A Spring Boot 4.0.2 application with Java 25 that provides search functionality via REST APIs. The goal of this
project is to use OpenSearch for various search practice projects.

## Features

- RESTful API for product search
- OpenSearch integration for full-text search
- Support for MATCH and TERM queries
- Filter by brand and category
- Pagination support
- OpenAPI documentation (Swagger UI)

## Prerequisites

- Java 25
- OpenSearch 3.x

## Build

```bash
./gradlew clean build
```

## Run

```bash
./gradlew bootRun
```

The application runs on port 8081 by default. Or specify a port in `src/main/resources/application.yml`:
```yaml
server:
  port: 8081
```

## Configuration

Configuration is managed via `src/main/resources/application.yaml`:

```yaml
open-search:
  protocol: http
  host: localhost
  port: 9200
  product:
    index-name: products

embedding:
  protocol: http
  host: localhost
  port: 8000
  path: embed
  char-limit: 2500
```

## API Endpoints

### Health Check

```
GET /index-health
```

Returns the OpenSearch cluster health status.

### Product Search

```
GET /v1/product/search?query=<query>&from=0&size=10&sort=<sort>&brand=<brand>&category=<category>&fields=<fields>
```

**Parameters:**
- `query` (required) - Search query string
- `from` (optional, default: 0) - Pagination offset
- `size` (optional, default: 10) - Number of results
- `sort` (optional, default: relevance) - Sort type: relevance, lexical, semantic, hybrid, price_asc, price_desc
- `brand` (optional) - Filter by brand
- `category` (optional) - Filter by category
- `fields` (optional, default: id,title) - Comma-separated list of fields to return

**Example:**
```
GET /v1/product/search?query=jacket&brand=Nike&size=5&sort=relevance&fields=id,title,price
```

## Documentation

OpenAPI/Swagger documentation is available at:
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/api-docs

## Testing

```bash
./gradlew test
```

Run specific test class:
```bash
./gradlew test --tests "SearchControllerTests"
```

## Project Structure

```
src/
├── main/
│   ├── java/org/example/search/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data transfer objects
│   │   ├── model/           # Domain models
│   │   ├── service/         # Service classes
│   │   └── util/            # Utility classes
│   └── resources/
│       └── application.yaml # Application configuration
└── test/
    └── java/                # Test classes
```
