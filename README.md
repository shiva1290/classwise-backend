# ClassWise Backend (Spring Boot)

REST API for the ClassWise university management platform. Built with Spring Boot 3, Spring Security, JPA/Hibernate, and PostgreSQL.

## Tech Stack

| Tool | Purpose |
|------|---------|
| Spring Boot 3 | Java backend framework |
| Spring Security | Session-based authentication |
| PostgreSQL | Relational database (Neon) |
| JPA / Hibernate | ORM & entity mapping |
| Flyway | Database migrations |
| Maven | Build & dependency management |
| Docker | Containerized deployment |

## API Endpoints

### Auth (public)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/sign-up/email` | Register a new user |
| POST | `/api/auth/sign-in/email` | Login |
| POST | `/api/auth/sign-out` | Logout |
| GET | `/api/auth/get-session` | Get current session |

### Users (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List users (paginated) |
| GET | `/api/users/:id` | Get user by ID |
| GET | `/api/users/:id/departments` | User's departments |
| GET | `/api/users/:id/subjects` | User's subjects |

### Departments (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/departments` | List departments |
| POST | `/api/departments` | Create department |
| GET | `/api/departments/:id` | Get department |
| GET | `/api/departments/:id/subjects` | Department subjects |
| GET | `/api/departments/:id/classes` | Department classes |
| GET | `/api/departments/:id/users` | Department users |

### Subjects (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/subjects` | List subjects |
| POST | `/api/subjects` | Create subject |
| GET | `/api/subjects/:id` | Get subject |
| GET | `/api/subjects/:id/classes` | Subject classes |
| GET | `/api/subjects/:id/users` | Subject users |

### Classes (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/classes` | List classes |
| POST | `/api/classes` | Create class |
| GET | `/api/classes/:id` | Get class |
| GET | `/api/classes/:id/users` | Class students |

### Enrollments (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/enrollments` | Enroll student |
| POST | `/api/enrollments/join` | Join via invite code |

### Stats (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/stats/overview` | Dashboard KPIs |
| GET | `/api/stats/latest` | Latest classes & teachers |
| GET | `/api/stats/charts` | Chart data |

### Health (public)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL database (local or [Neon](https://neon.tech))

### Setup

1. Copy `.env.example` to `.env` and fill in your database credentials:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/DBNAME
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=replace_me
```

2. Run the application:

```bash
mvn spring-boot:run
```

The server starts at http://localhost:8080. Flyway runs migrations automatically on startup.

### Build

```bash
mvn -DskipTests clean package
java -jar target/*.jar
```

## Deployment (Render)

### Using Docker

1. Push this repo to GitHub
2. Create a new **Web Service** on [Render](https://render.com)
3. Select **Docker** as the runtime
4. Set environment variables:
   - `SPRING_DATASOURCE_URL` — JDBC connection string
   - `SPRING_DATASOURCE_USERNAME` — DB username
   - `SPRING_DATASOURCE_PASSWORD` — DB password
   - `PORT` — Render sets this automatically
5. Deploy

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | Yes | JDBC PostgreSQL URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `PORT` | No | Server port (default: 8080) |

## Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@classwise.com | test123@ |
| Teacher | teacher@classwise.com | test123@ |
| Student | student@classwise.com | test123@ |
