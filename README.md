# Reviews Service (Simple MVC)

Minimal Spring Boot 2.7 + MySQL service that imports reviews at startup and exposes a small search API.

## Run

```bash
cp .env.example .env
# edit REVIEW_API_KEY in .env
docker-compose up --build
```

## Endpoints

- `GET /reviews?q=&source=&minRating=&maxRating=&fromDate=&toDate=&tag=&page=&size=&sort=reviewDate,desc`
- `GET /reviews/{id}`
- `DELETE /reviews/{id}`

## Config (env)
- `REVIEW_API_URL`
- `REVIEW_API_KEY`
- `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATASOURCE_URL` is set by compose to MySQL container.

## Notes
- Schema is managed by Hibernate: `spring.jpa.hibernate.ddl-auto=update` (no migrations).
- Startup importer is non-fatal: if the API fails, the app still starts.
