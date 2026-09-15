# 🚦 Smart Queue API

A **queue management REST API** for a small service center. Customers can join the queue and check their position, while authorized staff can call, complete, cancel, and manage customers.

Built as part of the **NV ProjectLab Backend Interview Task**.

---

## 🛠️ Tech Stack

* ☕ **Java 17**
* 🍃 **Spring Boot 3.3**
* 🗄️ **Spring Data JPA + PostgreSQL**
* 🧬 **Liquibase** — database schema migrations
* 🔐 **Spring Security + JWT** — admin authentication and authorization
* 🔄 **MapStruct** — entity ↔ DTO mapping
* 📖 **springdoc-openapi / Swagger UI**
* 🚦 **Bucket4j** — IP-based rate limiting
* 🧩 **Lombok**
* 🐳 **Docker / Docker Compose**

---

## ✨ Features

* ➕ Customers can join the queue
* 📋 View all waiting customers in queue order
* 📍 Check a customer's current position
* 📣 Admins can call the next customer
* ✅ Admins can complete a customer's service
* ❌ Admins can cancel a customer
* 🗑️ Admins can remove a customer
* 📊 Admin queue statistics
* 🕘 Paginated queue history
* 🔐 JWT-based admin authentication
* 🚦 IP-based API rate limiting
* 🧬 Liquibase database migrations
* 🔒 Concurrency protection for simultaneous `/next` requests
* 🐳 Dockerized application and database

---

# 🗄️ Database

The application uses **PostgreSQL**.

The database schema is managed entirely through **Liquibase**:

```text
src/main/resources/db/changelog/
```

### 👤 `customers`

| Column         | Description                  |
| -------------- | ---------------------------- |
| `id`           | UUID primary key             |
| `name`         | Customer name                |
| `phone`        | Customer phone number        |
| `status`       | Current queue status         |
| `created_at`   | Time the customer joined     |
| `updated_at`   | Last update time             |
| `called_at`    | Time the customer was called |
| `completed_at` | Time service was completed   |

### 👨‍💼 `admins`

| Column       | Description            |
| ------------ | ---------------------- |
| `id`         | UUID primary key       |
| `username`   | Unique admin username  |
| `password`   | BCrypt-hashed password |
| `role`       | Admin role             |
| `created_at` | Creation time          |
| `updated_at` | Last update time       |

An admin account is automatically seeded when the application starts if the `admins` table is empty.

The username and password are configured through environment variables:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

---

# 🚀 How to Run

## 🐳 Run with Docker Compose

The easiest way to run the complete application is with Docker Compose.

Docker Compose starts:

```text
Spring Boot application
        +
PostgreSQL database
```

---

## 1️⃣ Create your local `.env` file

Create a new file named:

```text
.env
```

in the **project root**, next to `docker-compose.yml`.

Do not commit this file to Git.

Example:

```env
POSTGRES_DB=smart_queue
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432

APP_PORT=8080

JWT_SECRET=change-this-to-a-long-random-secret-key
JWT_EXPIRATION_MS=3600000

ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123

RATE_LIMIT_CAPACITY=30
RATE_LIMIT_REFILL=30
RATE_LIMIT_DURATION=60
```

### 🔐 Important

The `JWT_SECRET` is required by the application to create and validate JWT tokens.

For local development, create your own secret:

```env
JWT_SECRET=your-own-long-random-secret
```

Do **not** use the example secret in a real environment.

The `.env` file should remain local and should be added to `.gitignore`:

```gitignore
.env
```

---

## 2️⃣ Start the application

From the project root:

```bash
docker compose up --build
```

Or run it in detached mode:

```bash
docker compose up -d --build
```

Docker will:

1. 🐘 Start PostgreSQL
2. ❤️ Wait until PostgreSQL is healthy
3. 🏗️ Build the Spring Boot application
4. 🚀 Start the application
5. 🧬 Run Liquibase migrations
6. 👨‍💼 Seed the admin account if necessary

The API will then be available at:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

## 3️⃣ Check the containers

```bash
docker compose ps
```

You should see:

```text
smart-queue-db
smart-queue-app
```

To view application logs:

```bash
docker compose logs -f app
```

To stop the application:

```bash
docker compose down
```

The PostgreSQL data remains in the Docker volume.

To remove the containers **and database volume**:

```bash
docker compose down -v
```

> ⚠️ `docker compose down -v` deletes the PostgreSQL Docker volume and therefore removes the stored database data.

---

# 🔐 Admin Login Flow

Administrative operations are protected using **Spring Security + JWT**.

Before logging in, you must create your own local `.env` file as described above.

The important variables for authentication are:

```env
JWT_SECRET=your-own-secret
JWT_EXPIRATION_MS=3600000

ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

These values are passed from Docker Compose to the Spring Boot application.

---

## 1️⃣ Configure Admin Credentials

In your local `.env`:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

You can use different credentials:

```env
ADMIN_USERNAME=myadmin
ADMIN_PASSWORD=my-secure-password
```

The admin account is seeded automatically when the application starts if the `admins` table is empty.

The password is stored in the database as a **BCrypt hash**, not as plain text.

---

## 2️⃣ Configure the JWT Secret

In the same `.env` file, provide a secret used for signing JWT tokens:

```env
JWT_SECRET=your-own-long-random-secret
```

The application uses this secret to:

* 🔏 Sign JWT tokens during login
* 🔍 Validate JWT tokens on protected requests

The secret should be sufficiently long and unpredictable.

---

## 3️⃣ Start the Application

After creating `.env`:

```bash
docker compose up --build
```

The environment variables from `.env` are passed to the `app` container.

The application can then authenticate the configured admin.

---

## 4️⃣ Login

Send:

```http
POST /api/auth/login
Content-Type: application/json
```

with the credentials configured in `.env`.

For the example configuration:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

If the credentials are valid, the API returns a JWT access token.

Example:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  },
  "timestamp": "..."
}
```

---

## 5️⃣ Use the JWT Token

Copy the returned token and include it in the `Authorization` header when calling protected endpoints:

```http
Authorization: Bearer <JWT_TOKEN>
```

For example:

```http
POST /api/queue/next
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 6️⃣ Admin Request Flow

The complete authentication flow is:

```text
┌──────────────────┐
│  Create .env     │
│                  │
│ ADMIN_USERNAME   │
│ ADMIN_PASSWORD   │
│ JWT_SECRET       │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Docker Compose   │
└────────┬─────────┘
         │
         ▼
┌────────────────────────┐
│    Spring Boot App     │
└────────────┬───────────┘
             │
             │ Admin login
             ▼
┌────────────────────────┐
│   POST /api/auth/login │
└────────────┬───────────┘
             │
             │ Validate credentials
             ▼
┌────────────────────────┐
│      PostgreSQL        │
│        admins          │
└────────────┬───────────┘
             │
             │ Valid
             ▼
┌────────────────────────┐
│       JWT Token        │
└────────────┬───────────┘
             │
             │ Bearer token
             ▼
┌────────────────────────┐
│ Protected API Endpoint │
│    /api/queue/next     │
└────────────┬───────────┘
             │
             ▼
          ✅ Allowed
```

Without a valid JWT:

```text
401 Unauthorized
```

With a valid JWT but insufficient permissions:

```text
403 Forbidden
```

---

# 📡 API Endpoints

## 🔑 Authentication

| Method | Endpoint          | Auth      | Description                        |
| ------ | ----------------- | --------- | ---------------------------------- |
| `POST` | `/api/auth/login` | 🌐 Public | Authenticate admin and receive JWT |

---

## 👥 Queue

| Method   | Endpoint                     | Auth      | Description                       |
| -------- | ---------------------------- | --------- | --------------------------------- |
| `POST`   | `/api/queue`                 | 🌐 Public | Add a new customer                |
| `GET`    | `/api/queue`                 | 🌐 Public | List waiting customers            |
| `GET`    | `/api/queue/{id}`            | 🌐 Public | Get customer and current position |
| `GET`    | `/api/queue/{id}/position`   | 🌐 Public | Get customer's position           |
| `POST`   | `/api/queue/next`            | 🔒 Admin  | Call the next waiting customer    |
| `POST`   | `/api/queue/{id}/complete`   | 🔒 Admin  | Complete a customer's service     |
| `POST`   | `/api/queue/{id}/cancel`     | 🔒 Admin  | Cancel a customer                 |
| `DELETE` | `/api/queue/{id}`            | 🔒 Admin  | Remove a customer                 |
| `GET`    | `/api/queue/stats`           | 🔒 Admin  | View queue statistics             |
| `GET`    | `/api/queue/history?status=` | 🔒 Admin  | View paginated queue history      |

---

# 📦 Response Format

Successful responses use a consistent response envelope:

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

Errors follow a consistent structure:

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

# 🚦 Rate Limiting

All `/api/**` requests are protected by an IP-based rate limiter using **Bucket4j**.

The default configuration allows:

```text
30 requests / minute / client IP
```

The following environment variables control the rate limiter:

```env
RATE_LIMIT_CAPACITY=30
RATE_LIMIT_REFILL=30
RATE_LIMIT_DURATION=60
```

When the configured limit is exceeded:

```http
429 Too Many Requests
```

The current implementation uses an **in-memory token bucket**, so the rate limiter is intended for a single application instance.

---

# 🔒 Concurrency: Preventing Duplicate `/next` Calls

One of the main design considerations of this task is handling simultaneous requests to:

```http
POST /api/queue/next
```

## ⚠️ The Problem

Suppose two staff members call `/next` at almost exactly the same time.

Without concurrency control:

```text
Request A ──┐
            ├──> Customer X
Request B ──┘
```

Both requests could read the same waiting customer before either request changes its status.

This could result in:

```text
❌ Customer X is called twice
❌ Customer Y remains waiting
```

---

## 🔐 Solution: Pessimistic Row-Level Locking

The application uses a **pessimistic database lock** inside a single transaction.

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

With PostgreSQL, this provides row-level locking equivalent to:

```sql
SELECT ...
FROM customers
WHERE status = 'WAITING'
ORDER BY created_at ASC
FOR UPDATE;
```

---

## 🔄 How It Works

### 1️⃣ Request A

Transaction A selects the first waiting customer:

```text
Customer X → 🔒 LOCKED
```

### 2️⃣ Request B

Transaction B arrives at the same time:

```text
Customer X → ⏳ WAITING FOR LOCK
```

### 3️⃣ Request A Updates the Customer

Transaction A changes:

```text
WAITING → SERVING
```

and commits.

### 4️⃣ Request B Continues

Transaction B continues and sees that Customer X is no longer waiting.

It therefore selects the next customer:

```text
Customer Y → SERVING
```

The result is:

```text
Request A → Customer X
Request B → Customer Y
```

instead of both requests receiving Customer X.

---

## ✅ Why Pessimistic Locking?

Pessimistic locking was selected because:

* PostgreSQL already provides row-level locking
* The critical operation is short
* The queue is relatively small
* Queue ordering remains deterministic
* No additional infrastructure is required
* PostgreSQL remains the source of truth

---

## 🔄 Alternative: `SKIP LOCKED`

PostgreSQL also supports:

```sql
FOR UPDATE SKIP LOCKED
```

This can improve throughput under heavy concurrency because requests do not wait for locked rows.

For this task, regular pessimistic locking is sufficient because `/next` represents a sequential queue operation where briefly waiting for the previous transaction is acceptable.

---

## 🟡 Optimistic Locking

JPA's `@Version` was also considered.

However, optimistic locking would require handling concurrent update failures and potentially retrying the operation.

For this relatively small queue, pessimistic locking provides a simpler solution.

---

## 🔴 Redis Distributed Lock

A Redis distributed lock could be useful in a larger distributed deployment.

However, the queue is stored in PostgreSQL, and PostgreSQL already provides the required locking mechanism.

Introducing Redis only for this operation would add unnecessary infrastructure for the current project.

---

# 🏗️ Architecture

The application follows a simple layered architecture:

```text
┌─────────────────────────────┐
│       REST Controller       │
│      HTTP / Validation      │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│           Service           │
│       Business Logic        │
│       Transactions          │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│         Repository          │
│        Spring Data JPA      │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│          PostgreSQL         │
└─────────────────────────────┘
```

Additional components:

```text
Spring Security
      ↓
JWT Authentication
      ↓
Protected Admin Endpoints

Liquibase
      ↓
Database Schema Migrations

Bucket4j
      ↓
API Rate Limiting

MapStruct
      ↓
Entity ↔ DTO Mapping
```

---

# 📖 API Documentation

Interactive API documentation is available through Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Swagger allows you to:

* 📖 Explore available endpoints
* 🧪 Send API requests
* 🔐 Authorize using a JWT
* 📦 Inspect request and response models
* ❗ Test API behavior

### 🔐 Using JWT in Swagger

1. Create your local `.env` file.
2. Configure `ADMIN_USERNAME`, `ADMIN_PASSWORD`, and `JWT_SECRET`.
3. Start the application.
4. Open Swagger UI.
5. Call:

```text
POST /api/auth/login
```

6. Copy the JWT returned by the login endpoint.
7. Click **Authorize** in Swagger.
8. Enter:

```text
Bearer <your-jwt-token>
```

9. Click **Authorize**.
10. You can now test the admin-only endpoints.

---

# 📁 Project Structure

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
│       └── application.yml
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .env
└── .gitignore
```

> `.env` is a local configuration file and should not be committed to the repository.

---

# 🎯 Project Goals

This project demonstrates practical backend development concepts including:

* ☕ Java and Spring Boot
* 🌐 REST API design
* 🗄️ PostgreSQL and JPA
* 🧬 Liquibase database migrations
* 🔐 JWT authentication
* 🛡️ Spring Security authorization
* 🔒 Database-level concurrency control
* 🚦 API rate limiting
* 🔄 Transaction management
* 🔁 DTO mapping with MapStruct
* 📖 OpenAPI / Swagger documentation
* 🐳 Dockerized application deployment

---

## 👨‍💻 Author

**Elşən Həsənov**

Java Backend Developer

Built for the **NV ProjectLab Backend Interview Task**.
