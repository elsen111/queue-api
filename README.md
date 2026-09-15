# 🚦 Smart Queue API

A **queue management REST API** for a small service center — customers join a queue, staff call the next customer, and everyone can check their live position.

Built as part of the **NV ProjectLab Backend Interview Task**.

---

## 🛠️ Tech Stack

* ☕ **Java 17**
* 🍃 **Spring Boot 3.3**
* 🗄️ **Spring Data JPA + PostgreSQL**
* 🧬 **Liquibase** — database schema migrations
* 🔐 **Spring Security + JWT** — admin authentication
* 🔄 **MapStruct** — entity ↔ DTO mapping
* 📖 **springdoc-openapi / Swagger UI**
* 🚦 **Bucket4j** — API rate limiting
* 🧩 **Lombok**
* 🧪 **JUnit 5 + Mockito + MockMvc**
* 🐳 **Docker / Docker Compose**

---

## 🗄️ Database

The application uses **PostgreSQL**, with the database schema managed entirely by **Liquibase**.

Changelogs are located at:

```text
src/main/resources/db/changelog
```

### 👤 `customers`

| Column         | Description                        |
| -------------- | ---------------------------------- |
| `id`           | UUID primary key                   |
| `name`         | Customer name                      |
| `phone`        | Customer phone number              |
| `status`       | Current queue status               |
| `created_at`   | Time the customer joined the queue |
| `updated_at`   | Last update time                   |
| `called_at`    | Time the customer was called       |
| `completed_at` | Time service was completed         |

### 👨‍💼 `admins`

| Column       | Description            |
| ------------ | ---------------------- |
| `id`         | UUID primary key       |
| `username`   | Unique admin username  |
| `password`   | BCrypt-hashed password |
| `role`       | Admin role             |
| `created_at` | Creation time          |
| `updated_at` | Last update time       |

A default admin is seeded automatically on first startup if the `admins` table is empty.

Default credentials:

```text
Username: admin
Password: admin123
```

They can be overridden using:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

---

## 🚀 How to Run

### 🐳 With Docker — Recommended

Build and start the application together with PostgreSQL:

```bash
docker compose up --build
```

Or run it in detached mode:

```bash
docker compose up -d --build
```

The API will be available at:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Check running containers:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f app
```

Stop the application:

```bash
docker compose down
```

---

### 💻 Locally

1. Start PostgreSQL, or start only the database using Docker:

```bash
docker compose up db
```

2. Set the required environment variables, or use the defaults configured in `application.yml`:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

3. Run the application:

```bash
mvn spring-boot:run
```

---

## 🧪 Tests

Run the complete test suite with:

```bash
mvn test
```

The project uses:

* JUnit 5
* Mockito
* MockMvc
* Spring Security Test

---

## 🔐 Authentication

The application uses **JWT-based authentication** for administrator operations.

### 🔑 Login

Send a request to:

```http
POST /api/auth/login
```

with the admin credentials.

The response contains a JWT token.

Use the token in subsequent admin requests:

```http
Authorization: Bearer <token>
```

### 🌐 Public Endpoints

The following operations do not require authentication:

* Join the queue
* View waiting customers
* View a customer
* Check customer position

### 🔒 Admin-only Endpoints

The following operations require authentication:

* Call next customer
* Remove customer
* Complete customer service
* Cancel customer
* View statistics
* View queue history

---

## 📡 API Endpoints

| Method       | Endpoint                     | Auth     | Description                            |
| ------------ | ---------------------------- | -------- | -------------------------------------- |
| 🔑 `POST`    | `/api/auth/login`            | —        | Admin login and JWT generation         |
| ➕ `POST`     | `/api/queue`                 | —        | Add a new customer to the queue        |
| 📋 `GET`     | `/api/queue`                 | —        | List all waiting customers in order    |
| 👤 `GET`     | `/api/queue/{id}`            | —        | Get a customer and current position    |
| 📍 `GET`     | `/api/queue/{id}/position`   | —        | Get only the customer's position       |
| 📣 `POST`    | `/api/queue/next`            | 🔒 Admin | Call the next waiting customer         |
| ✅ `POST`     | `/api/queue/{id}/complete`   | 🔒 Admin | Mark a served customer as completed    |
| ❌ `POST`     | `/api/queue/{id}/cancel`     | 🔒 Admin | Cancel a customer's place in the queue |
| 🗑️ `DELETE` | `/api/queue/{id}`            | 🔒 Admin | Remove a customer completely           |
| 📊 `GET`     | `/api/queue/stats`           | 🔒 Admin | Get live queue statistics              |
| 🕘 `GET`     | `/api/queue/history?status=` | 🔒 Admin | Get paginated queue history            |

---

## 📦 Response Format

All successful responses use a consistent response envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {
    "...": "..."
  },
  "timestamp": "..."
}
```

### ❗ Error Response

Errors use a consistent structure as well:

```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id: ...",
  "path": "/api/queue/..."
}
```

---

## 🚦 Rate Limiting

Every `/api/**` request is rate-limited by **client IP address** using an in-memory **Bucket4j token bucket**.

### ⚙️ Default Configuration

```text
30 requests / minute
```

The following environment variables can be used to configure the limit:

```text
RATE_LIMIT_CAPACITY
RATE_LIMIT_REFILL
RATE_LIMIT_DURATION
```

When the limit is exceeded, the API returns:

```http
429 Too Many Requests
```

---

## 🔒 Concurrency: Preventing Duplicate `/next` Calls

One of the core design questions of the task is handling two simultaneous requests to:

```http
POST /api/queue/next
```

### ⚠️ The Problem

Imagine two staff members call `/next` at almost exactly the same time.

Without proper concurrency control:

```text
Request A ──> Find next WAITING customer ──> Customer X
Request B ──> Find next WAITING customer ──> Customer X
```

Both requests could read the same customer before either request updates the status.

The result:

```text
❌ Customer X is called twice
❌ The next customer remains waiting
```

---

## 🔐 Solution: Pessimistic Database Locking

The application uses a **pessimistic row-level lock** inside a single database transaction.

The repository uses:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    select c
    from CustomerEntity c
    where c.status = :status
    order by c.createdAt asc
""")
List<CustomerEntity> lockNextWaiting(QueueStatus status);
```

With PostgreSQL, this results in row-level locking similar to:

```sql
SELECT ...
FROM customers
WHERE status = 'WAITING'
ORDER BY created_at ASC
FOR UPDATE;
```

### 🔄 How It Works

#### 1️⃣ Transaction A

Request A calls `/next`.

The database locks the first `WAITING` customer.

```text
Customer X → 🔒 LOCKED
```

#### 2️⃣ Transaction B

Request B arrives at the same time.

It attempts to acquire the same lock:

```text
Customer X → ⏳ WAITING FOR LOCK
```

#### 3️⃣ Transaction A Updates the Customer

Transaction A changes:

```text
WAITING → SERVING
```

and commits.

```text
Customer X → SERVING
```

#### 4️⃣ Transaction B Continues

Transaction B can now proceed.

Because Customer X is no longer `WAITING`, it selects the next customer:

```text
Customer Y → WAITING
```

and changes:

```text
WAITING → SERVING
```

### ✅ Result

The database effectively serializes concurrent `/next` operations:

```text
Request A ──> Customer X ──> SERVING
                         ↓
                      COMMIT
                         ↓
Request B ──> Customer Y ──> SERVING
```

Therefore:

> **The same customer cannot be successfully called twice by concurrent `/next` requests.**

If there are no waiting customers, the service returns:

```http
409 Conflict
```

---

## ⚖️ Why Pessimistic Locking?

### 🟢 Pessimistic Locking

Chosen because:

* PostgreSQL already provides row-level locking
* The critical operation is very short
* The queue is small
* The operation needs deterministic ordering
* No additional infrastructure is required
* Works correctly even when multiple application threads access the database concurrently

---

### 🟡 `SKIP LOCKED`

PostgreSQL also supports:

```sql
FOR UPDATE SKIP LOCKED
```

This can provide higher throughput under heavy concurrency because a transaction doesn't wait for a locked row. Instead, it skips it and processes another available row.

For this application, however, regular pessimistic locking is sufficient because `/next` is a sequential queue operation and briefly waiting for the lock preserves deterministic behavior.

---

### 🟠 Optimistic Locking

Optimistic locking with JPA `@Version` was also considered.

However, it would require handling concurrent update failures and potentially retrying the operation.

For this small queue, that adds complexity without providing a meaningful benefit over a short database lock.

---

### 🔴 Redis Distributed Lock

A Redis-based distributed lock could be useful in a more complex distributed architecture.

However, for this application:

```text
Application
     ↓
PostgreSQL
```

the database is already the source of truth for the queue.

Therefore, a PostgreSQL row-level lock is simpler and avoids introducing another infrastructure dependency.

---

## 🏗️ Architecture

The application follows a layered architecture:

```text
┌──────────────────────────┐
│       REST Controller    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│          Service         │
│   Business Logic / TX    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│        Repository        │
│       Spring Data JPA    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│        PostgreSQL        │
└──────────────────────────┘
```

Database schema changes are managed separately through:

```text
Liquibase
   ↓
PostgreSQL
```

---

## 📚 API Documentation

Interactive API documentation is available through **Swagger UI**:

```text
http://localhost:8080/swagger-ui.html
```

Swagger can be used to:

* 📖 Explore available endpoints
* 🧪 Send API requests
* 🔐 Authorize using a JWT
* 📦 Inspect request/response schemas
* ❗ Test error responses

---

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/nvprojectlab/smartqueue/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── exception/
│   │       ├── mapper/
│   │       ├── repository/
│   │       ├── security/
│   │       └── service/
│   │
│   └── resources/
│       ├── db/
│       │   └── changelog/
│       └── application.yml/
│
└── test/
    └── java/
```

---

## 🎯 Project Goals

The project focuses on demonstrating:

* ☕ Java and Spring Boot fundamentals
* 🌐 REST API design
* 🗄️ Relational database usage
* 🔄 Transaction management
* 🔐 Authentication and authorization
* 🔒 Database-level concurrency control
* 🧪 Automated testing
* 🧬 Database migration management
* 🐳 Containerized development
* 📖 API documentation
* ⚡ Basic API protection through rate limiting

---

## 👨‍💻 Author

**Elşən Həsənov**

Java Backend Developer

Built for the **NV ProjectLab Backend Interview Task**.
