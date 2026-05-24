# Journal App

A simple Spring Boot REST API application for managing journal entries with CRUD operations.

## Features

- Create journal entries with auto-generated unique IDs based on timestamp + random suffix
- Retrieve all journal entries
- RESTful API endpoints
- In-memory storage using HashMap

## Tech Stack

- **Framework**: Spring Boot 2.7.16
- **Language**: Java
- **Build Tool**: Maven
- **Web Server**: Apache Tomcat (embedded)
- **Port**: 8081

## Project Structure

```
journalApp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── net/engineeringdigest/journalApp/
│   │   │       ├── JournalApplication.java (Main Spring Boot entry point)
│   │   │       ├── controller/
│   │   │       │   ├── HealthCheckController.java (Health check endpoint)
│   │   │       │   └── JournalEntryController.java (Journal CRUD endpoints)
│   │   │       └── entity/
│   │   │           └── JournalEntry.java (Model class)
│   │   └── resources/
│   │       └── application.properties (Configuration)
│   └── test/
│       └── java/ (Test classes)
├── pom.xml (Maven dependencies)
└── README.md (This file)
```

## Getting Started

### Prerequisites

- Java 11 or higher (currently using Java 25)
- Maven 3.8.7 or later

### Installation & Setup

1. **Clone the repository**:
```bash
git clone git@github.com:KarthikBenki/JournalApp.git
cd journalApp
```

2. **Build the project**:
```bash
./mvnw clean install
```

3. **Run the application**:
```bash
./mvnw spring-boot:run
```

The server will start on `http://localhost:8081`

## API Endpoints

### Health Check
- **GET** `/info`
  - Returns: `"Journal App is running"`
  - Example: `http://localhost:8081/info`

### Journal Entries

- **GET** `/journal`
  - Retrieves all journal entries
  - Response: List of `JournalEntry` objects

- **POST** `/journal`
  - Creates a new journal entry
  - Request Body (JSON):
    ```json
    {
      "title": "My First Entry",
      "content": "Today was a great day!",
      "date": "2026-05-24"
    }
    ```
  - Response: The created `JournalEntry` with auto-generated ID

- **GET** `/journal/id/{id}`
  - Retrieves a single journal entry by ID
  - Example: `http://localhost:8081/journal/id/1653456789000123456`

- **PUT** `/journal/id/{id}`
  - Updates an existing journal entry with the given ID
  - Request Body (JSON): same shape as POST; the server will use the URL id for the update
  - Response: The updated `JournalEntry`

- **DELETE** `/journal/id/{id}`
  - Deletes the journal entry with the given ID
  - Response: boolean true on success, false if not found (note: consider switching to proper HTTP codes)

## ID Generation

IDs are generated using timestamp + random suffix to ensure:
- Uniqueness within the application lifetime
- Rough time-ordering of entries
- Very low collision probability (supports 1M inserts per millisecond)

Formula: `(currentEpochMillis * 1_000_000) + randomSuffix`

## Configuration

Edit `src/main/resources/application.properties` to customize:

```properties
server.port=8081
```

## Testing

Run the test suite:
```bash
./mvnw test
```

## Example Usage

### Create an entry using curl:
```bash
curl -X POST http://localhost:8081/journal \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Day 1",
    "content": "Starting my journal app journey"
  }'
```

### Get all entries:
```bash
curl http://localhost:8081/journal
```

### Check health:
```bash
curl http://localhost:8081/info
```

## Future Enhancements

- Database integration (JPA/Hibernate)
- User authentication
- Entry search and filtering
- Soft delete functionality
- REST API documentation (Swagger/OpenAPI)

## License

MIT License

## Author

Engineering Digest

