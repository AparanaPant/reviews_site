# Reviews Service

A Spring Boot service that **imports and stores customer reviews** from a single upstream Review API and exposes a REST API to **search, retrieve, and delete** reviews.

- Runs with `docker-compose up`
- Imports all (new) reviews on startup
- Persists data to MySQL
- Clean DTO mapping with MapStruct + Lombok
- Config managed through environment variables (dotenv supported for local dev)

---

## ⚠️ Notes on the Provided Upstream API

**Pagination mismatch**  
The mock API’s pagination metadata (`page`, `size`, `totalPages`) can be inconsistent — for example, `totalPages` may not reflect small changes in total record count. In this system, we rely on the upstream `totalPages` as given, even if it’s not always perfectly accurate.

**Tags**  
The upstream API defines the field as `tags` (plural) but always returns a single string value (e.g., `"SERVICE"`, `"SALES"`). To keep things simple, we store it as a plain string instead of forcing it into a collection.

---

## Tech Choices / Compatibility Notes

- **Spring Boot version**: I used **Spring Boot 2.7.x** because the supplied `docker-compose.yml` specifies:  
  ```
  image: maven:3.8.1-openjdk-16
  ```  
  which locks the project to JDK 16.  
  - Spring Boot 3.x requires at least JDK 17, so upgrading would break compatibility with the provided container setup.  
  - In a real-world project, I would likely move to **Spring Boot 3.x + JDK 17+** to benefit from newer Jakarta APIs, performance improvements, and longer support windows.

---

## Features

- Import on startup (fetch → batch upsert into MySQL)
- REST API:
  - `GET /reviews` — list/search (paginated)
  - `GET /reviews/{id}` — fetch one
  - `DELETE /reviews/{id}` — delete one
- Config via `.env` (dotenv loaded before Spring for local dev)
- Proper logging & error handling
- Reusable mapper and DTOs (Lombok `@Value @Builder`, MapStruct)
- Docker Compose: database + service

---

## 🔌 REST API

### 1) List/Search All Reviews (Paginated)

```
GET /reviews?page=1&pageSize=10
```

**Query params**
- `page` — **1-based** page index (e.g., `1`, `2`, …)
- `pageSize` — items per page (e.g., `10`, `50`, `100`)
- (Optional, future-friendly) filters:  
  `source`, `author`, `rating`, `tag`, `fromDate`, `toDate`

**Example `curl`**
```bash
curl -X GET "http://localhost:3000/reviews?page=1&pageSize=10"
```

**Example response (trimmed)**

```json
{
  "page": 1,
  "totalPages": 15,
  "totalElements": 149,
  "pageSize": 10,
  "items": [
    {
      "id": 1751,
      "source": "YELP",
      "externalId": "6e775de5-46d4-4e46-8055-1ebd486a2d05",
      "author": "Ben Nicolas",
      "rating": 5,
      "content": "Dolorem nihil sit. Quia corrupti perferendis.",
      "reviewDate": "2025-09-25T21:16:21.258",
      "tag": "SERVICE"
    },
    {
      "id": 451,
      "source": "FACEBOOK",
      "externalId": "e559cab2-eb9a-4c55-845e-c6ef0a7911cf",
      "author": "Dr. Derrick Mayert",
      "rating": 3,
      "content": "Ut est sunt corporis alias est et sequi voluptas.",
      "reviewDate": "2025-09-26T08:36:11.246",
      "tag": "SALES"
    }
  ]
}
```

**Pagination fields (returned by our API)**
- `page` — current page (1-based)
- `pageSize` — requested page size
- `totalElements` — total count of matched reviews in our DB
- `totalPages` — computed from `totalElements` and `pageSize`
- `items` — list of reviews for this page

> We keep these fields consistent regardless of upstream quirks.

---

### 2) Get Review by ID

```
GET /reviews/{id}
```

**Example**
```bash
curl -X GET "http://localhost:3000/reviews/124"
```

---

### 3) Delete Review by ID

```
DELETE /reviews/{id}
```

**Example**
```bash
curl -X DELETE "http://localhost:3000/reviews/124"
```

---

## Design Assumptions

- Real-world ingestion would come from **multiple sources** (Google, Yelp, DealerRater, Facebook, Cars.com, etc.) consolidated by a **single upstream API** (likely powered by crawlers/aggregators).  
- Our service treats the upstream as a single provider and focuses on **importing and storing** that data reliably.
- The assignment only requires **reviews** search; fields unrelated to reviews (e.g., business name) were **intentionally not stored** to keep scope tight and requirements satisfied.
- The upstream exposes a single string `tag`; although tags are often lists, we store it **as-is** (string).

---

## Running the System

### Prerequisites
- Docker
- Docker Compose

### Environment Variables
We support dotenv for local dev. Create `.env.local` (or use `.env.example` as a template):

```
SERVER_PORT=3000

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/interview?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=interview

LOG_LEVEL_ROOT=INFO

REVIEW_API_URL=https://obm8fcrdr1.execute-api.us-east-1.amazonaws.com/production/reviews
REVIEW_API_KEY=replace_me

# Pagination default used by the importer and/or API defaults
REVIEWS_PER_PAGE=500
```

> For local non-Docker runs, your JDBC URL host will likely be `localhost` and port `3307` if you kept the compose defaults.

### Start
```bash
docker-compose up --build
```

What happens:
1. MySQL container starts
2. App container starts
3. On startup, the app fetches reviews from the upstream API (authenticated with `x-api-key`) and stores them
4. REST endpoints available at `http://localhost:3000`

---

## How This Meets the Assignment

- **Import on startup**: implemented — we page through upstream data and batch upsert into MySQL.
- **REST API**:  
  - `GET /reviews` — returns paginated results with metadata
  - `GET /reviews/{id}` — returns a single review
  - `DELETE /reviews/{id}` — deletes a single review
- **Docker Compose**: includes MySQL + app; `docker-compose up` runs the full stack.

---

## System Diagram

```mermaid
flowchart TD
    A[External Reviews API] -->|fetch on startup| B[Importer (Spring Boot)]
    B -->|batch upsert| C[(MySQL)]
    C -->|query| D[REST API Layer]
    D --> E1[GET /reviews]
    D --> E2[GET /reviews/{id}]
    D --> E3[DELETE /reviews/{id}]
    E1 --> F[Clients / curl / UI]
    E2 --> F
    E3 --> F
```

---

## Future Improvements for Scale

If this grows to **millions of reviews** or runs in a **distributed** environment:

1. **Bulk Insert Optimization**
   - Use multi-row `INSERT ... ON DUPLICATE KEY UPDATE`
   - Batch sizes of **500–2,000** rows per transaction
   - Ensure JDBC driver batching: `rewriteBatchedStatements=true`
   - For JPA: `hibernate.jdbc.batch_size`, `order_inserts`, `order_updates`, and periodic `flush()/clear()`

2. **Staging (Temp) Tables — Practical, Low-Risk Path** ✅  
   - Load each import batch into a **staging table** first
   - Validate/clean in staging
   - Merge into the main `reviews` table with a single `INSERT ... SELECT` (or upsert)
   - Truncate staging after success; on failure, just retry the batch

3. **Asynchronous & Distributed Imports**
   - Use a queue/stream (Kafka/RabbitMQ/SQS)
   - Partition by source or ID range; multiple workers ingest concurrently

4. **DB-Specific Strategies**
   - For huge one-time loads: temporarily disable secondary indexes and rebuild after
   - Consider `LOAD DATA INFILE` for massive raw inserts
   - For heavy analytics/search, mirror to an analytical store (BigQuery/Snowflake)

> For this assignment, we intentionally kept it simple with **paged fetch + batch upsert**, which meets the requirements cleanly. The **staging-table approach** is the next sensible step if the system needs to scale.

