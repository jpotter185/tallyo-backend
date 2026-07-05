# Tallyo Backend

Spring Boot backend for the Tallyo sports dashboard. The service ingests ESPN scoreboard and summary data, stores normalized game/team/stat records in PostgreSQL, and exposes the API consumed by the Tallyo frontend hosted on Vercel.

## What It Does

- Fetches game schedules, live details, scores, odds, and stats from ESPN.
- Stores normalized games, teams, odds, and stat values in PostgreSQL.
- Serves paginated game data for league pages.
- Serves per-game details (box score stats, scoring plays, stat leaders).
- Serves league standings fetched from ESPN.
- Computes current league context, including current week/date.
- Provides league metadata so the frontend can adjust UI behavior per sport.
- Protects API routes with an `x-api-key` header.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- Maven wrapper
- Docker Compose for server deployment

## Project Structure

```text
src/main/java/com/tallyo/tallyo_backend
├── config       # Security, CORS, cache, ESPN config, request logging
├── controller   # REST API controllers
├── dto          # API response/request DTOs
├── entity       # JPA entities
├── enums        # League and sport metadata
├── exception    # API error handling
├── mapper       # ESPN model -> entity and entity -> DTO mapping
├── model        # ESPN response models
├── repository   # JPA repositories and custom queries
└── service      # ESPN ingestion, calendar/context, game queries
```

## Requirements

- Java 17
- Docker and Docker Compose, if running with PostgreSQL locally
- PostgreSQL, if running without Docker

## Environment Variables

The app reads configuration from environment variables.

| Variable | Required | Description |
| --- | --- | --- |
| `API_KEY` | Yes | API key required in the `x-api-key` request header. |
| `SPRING_DATASOURCE_URL` | Local default available | JDBC URL. Defaults to `jdbc:postgresql://localhost:5432/postgres`. |
| `SPRING_DATASOURCE_USERNAME` | Local | PostgreSQL username. |
| `SPRING_DATASOURCE_PASSWORD` | No | PostgreSQL password. Defaults to empty. |
| `DB_NAME` | Docker | PostgreSQL database name used by `docker-compose.yml`. |
| `DB_USER` | Docker | PostgreSQL user used by `docker-compose.yml`. |
| `DB_PASSWORD` | Docker | PostgreSQL password used by `docker-compose.yml`. |
| `DDL_AUTO` | Docker | Hibernate schema behavior, for example `update`. |

## Local Development

Start PostgreSQL first, then run the application:

```bash
export API_KEY=dev-secret
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

Compile without running tests:

```bash
./mvnw -q -DskipTests compile
```

Run tests:

```bash
./mvnw test
```

## Docker Deployment

The included Compose setup runs:

- Spring backend
- PostgreSQL
- Dozzle log viewer

`docker-compose.override.yml` publishes the app on `localhost:8080` for local development; it is not used in prod.

Build and start everything:

```bash
./mvnw clean package
docker compose up --build -d
```

Or use the deployment helper:

```bash
./deploy.sh
```

The deployment script builds the jar, recreates the Compose stack, prints container status, and tails logs.

## API Authentication

All API requests require:

```http
x-api-key: <API_KEY>
```

Example:

```bash
curl "http://localhost:8080/api/v1/leagues" \
  -H "x-api-key: dev-secret"
```

## API Overview

Base URL:

```text
/api/v1
```

Main endpoints:

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/leagues` | Returns supported leagues and frontend capability metadata. |
| `GET` | `/games` | Returns paginated games for a league, optionally filtered by year, season type, week, or date. |
| `GET` | `/games/current` | Returns games for the current league context. |
| `GET` | `/games/context` | Returns current year, season type, week, and date for a league. |
| `GET` | `/games/dates` | Returns known game dates for a league in the requested timezone. |
| `GET` | `/games/nhl-dates` | Legacy NHL-specific date endpoint. |
| `GET` | `/games/{gameId}/details` | Returns box score stats, scoring plays, and stat leaders for a game. |
| `GET` | `/standings` | Returns standings groups for a league. |
| `POST` | `/games` | Manually fetches and stores games from ESPN. |

Supported leagues are defined in `League.java`.

Current supported league IDs:

- `world_cup`
- `mls`
- `nfl`
- `cfb`
- `nhl`
- `mlb`

## Common Requests

Get league metadata:

```bash
curl "http://localhost:8080/api/v1/leagues" \
  -H "x-api-key: dev-secret"
```

Get current NFL games:

```bash
curl "http://localhost:8080/api/v1/games/current?league=nfl" \
  -H "x-api-key: dev-secret"
```

Get games for a specific date:

```bash
curl "http://localhost:8080/api/v1/games?league=nhl&date=2026-02-16&userTimeZone=America/New_York" \
  -H "x-api-key: dev-secret"
```

Get details for a single game:

```bash
curl "http://localhost:8080/api/v1/games/401671789/details" \
  -H "x-api-key: dev-secret"
```

Get standings for a league:

```bash
curl "http://localhost:8080/api/v1/standings?league=nhl" \
  -H "x-api-key: dev-secret"
```

Fetch and store games for a league/year:

```bash
curl -X POST "http://localhost:8080/api/v1/games?league=nfl&year=2026&shouldFetchStats=false" \
  -H "x-api-key: dev-secret"
```

Fetch games and attach box score stats:

```bash
curl -X POST "http://localhost:8080/api/v1/games?league=nfl&year=2026&shouldFetchStats=true" \
  -H "x-api-key: dev-secret"
```

## Data Refresh Behavior

The backend has a scheduled refresh job that checks whether any stored games need updates. When active games or due scheduled games exist, it fetches recent ESPN data and saves updated records.

For new seasons, backfills, or stale production data, use the manual update endpoint:

```bash
curl -X POST "https://api.tallyo.us/api/v1/games?league=nfl&year=2026" \
  -H "x-api-key: <API_KEY>"
```

Current-context responses are cached briefly and evicted every five minutes.

## API Contract

The OpenAPI contract lives in:

```text
openapi.yaml
```

When API behavior changes:

1. Update backend code.
2. Update `openapi.yaml`.
3. Regenerate frontend API types in the Tallyo frontend repo.
4. Deploy backend before relying on new frontend behavior.

## CORS

CORS is configured for:

```text
https://tallyo.us
```

If the frontend deployment URL changes, update `SecurityConfig`.

## Notes

- Hibernate is configured with `ddl-auto: update`, which is convenient for the current deployment but should be handled carefully as the schema matures.
- ESPN response models are local Java models under `model/espn`.
- OpenAPI is maintained manually.
- There are currently no committed tests under `src/test`.
