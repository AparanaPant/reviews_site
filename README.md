# Reviews Service

A small Spring Boot app that **imports customer reviews** from one upstream API and exposes a REST API to **list, fetch, and delete** reviews. Scope is intentionally tight: I store only what’s needed for the assignment in a single `reviews` table.

---

## Quick start

```bash
docker compose up
```
The app boots, hits the upstream API, and upserts reviews into MySQL. You can watch the importer logs, e.g.

```
2025-09-26 11:20:58.088  INFO ...ReviewImportService - Starting reviews import ... (startingPage=1, pageSize=500)
2025-09-26 11:21:02.019  INFO ...ReviewImportService - Page 1/3: received=50, batched=50, skipped=0, affected=50
2025-09-26 11:21:02.153  INFO ...ReviewImportService - Page 2/3: received=50, batched=50, skipped=0, affected=50
2025-09-26 11:21:02.335  INFO ...ReviewImportService - Page 3/3: received=50, batched=50, skipped=0, affected=50
```

> The env is already wired in the repo. Inside Docker, the app connects to `db:3306`. If you expose MySQL on a different **host** port for your tools (e.g., `3307`), that doesn’t affect the in-Docker connection.

---
## ⚠️ Upstream quirks

-The mock API always returns `totalPages = 3`  no matter what `size` I ask for, so I can’t fetch a single 500-record page and batch-insert 500 at once.
In a typical API, `size=500` would return up to 500 on page 1 (then 501+ on page 2) and I’d upsert in 500-sized batches; despite the quirk, this still works here because the dataset is only 150 (3×50).

## REST API

### List / search
```
GET /reviews?page=1&pageSize=10&source=GOOGLE&tag=SERVICE
```
Example:
```bash
curl "http://localhost:3000/reviews?page=1&pageSize=10&source=GOOGLE&tag=SERVICE"
```
Response (trimmed):
```json
{
  "page": 1,
  "pageSize": 10,
  "totalElements": 149,
  "totalPages": 15,
  "items": [
    {
      "id": 451,
      "source": "FACEBOOK",
      "externalId": "e559cab2-eb9a-4c55-845e-c6ef0a7911cf",
      "author": "Dr. Derrick Mayert",
      "rating": 3,
      "content": "Ut est sunt corporis alias...",
      "reviewDate": "2025-09-26T08:36:11.246",
      "tag": "SALES"
    }
  ]
}
```

### Get by id
```
GET /reviews/{id}
```
```bash
curl "http://localhost:3000/reviews/124"
```

### Delete by id
```
DELETE /reviews/{id}
```
```bash
curl -X DELETE "http://localhost:3000/reviews/124"
```

---


---

### Why upsert? (details also in code comments)

- The upstream can resend the same review with changes (fixed rating, edited text, updated tag).
- Since our business model depends on customer reviews, the **latest version** of a review is what matters most.  
  Ignoring duplicates would keep outdated data around, which reduces accuracy and increases costs.  
  By using `INSERT ... ON DUPLICATE KEY UPDATE`, we ensure that new data overwrites old instead of being ignored.
- The API does not provide any reliable incremental cursor or identifier to differentiate between “already pulled” and “updated” data.  
  Without such a mechanism, upsert is the most reliable way to stay current.
- Upsert keeps the DB in sync and avoids duplicates, aligning directly with the needs of a reviews-based business.

---

### Future improvements
If data volume grows, I’d introduce a staging table (load → validate → merge) or shift updates into async workers.  
For the current scope, upsert is the cleanest and most cost-effective solution.


## Data model

### What I ship (by design)
A single table because the requirement is only “search reviews.”

`reviews`  
- `id` (PK, auto-increment `INT`)  
- `source` (`VARCHAR`)  
- `external_id` (`VARCHAR`) — forms a unique key with `source`  
- `author`, `rating`, `content`, `review_date`, `tag`  
- `created_at`, `updated_at`

> Unique constraint: (`source`, `external_id`) so upserts are deterministic.

### How I’d expand if scope grows
- `sources(id, name, ...)`
- `reviews(id, source_id FK, external_id, author, rating, content, review_date, created_at, updated_at, ...)`
- `tags(id, name)` and `review_tags(review_id, tag_id)` if tags become multi-valued
- `customers(...)` or `businesses(...)` if identity and multi-tenant cases appear
- **IDs**: today `INT` is fine (~hundreds). At larger/distributed scale, I’d switch to **UUID v4/v7**.

---

### Tech & compatibility

- **Initial setup:** Started with **Spring Boot 2.7.x** on **JDK 16** (to align with the provided base image).
- **Adjustment:** Moved to **JDK 17** because it is an **LTS release** with broader community and vendor support, while still being fully compatible with Spring Boot 2.7.x.
- **Rationale:** Using 17 ensures long-term stability, security updates, and better tooling/library compatibility, without forcing an upgrade to Boot 3.x.


## Why this meets the brief

I load from a single upstream, store only what’s needed, expose the three required endpoints, and keep the system easy to run: `docker compose up`. The importer is safe (upsert + validation), fast enough for the data size (batching + size=500), and leaves clear headroom to evolve (staging tables, async ingestion, richer schema) if the product grows.
