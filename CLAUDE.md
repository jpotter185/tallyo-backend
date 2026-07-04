# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Spring Boot backend for the Tallyo sports dashboard. It ingests ESPN scoreboard/summary data, normalizes it into PostgreSQL, and serves a paginated game API consumed by the Tallyo frontend (Vercel). There is a sibling frontend repo at `../tallyo` that generates TypeScript types from `openapi.yaml`.

## Commands

```bash
./mvnw -q -DskipTests compile   # compile only
./mvnw test                     # run tests (there are currently none under src/test)
./mvnw spring-boot:run           # run locally, requires env vars below
./mvnw clean package             # build jar for deployment
./deploy.sh                      # build + docker compose down/up + tail logs (used in prod, requires .env)
```

Required env vars for local run: `API_KEY`, `SPRING_DATASOURCE_USERNAME` (password defaults to empty, URL defaults to `jdbc:postgresql://localhost:5432/postgres`).

Runs on `http://localhost:8080`. Every API request needs header `x-api-key: <API_KEY>`.

## Architecture

Package root: `com.tallyo.tallyo_backend`. Flow for a request: `controller` → `service` (business logic, calls `EspnService` for live data or `GameRepository` for stored data) → `mapper` converts between ESPN `model` objects, JPA `entity` objects, and response `dto` objects.

- `model/espn/scoreboard`, `model/espn/box_score` — hand-written POJOs mirroring ESPN's JSON responses (scoreboard = schedule/scores/odds, box_score = per-game stats fetched from the `summary` endpoint). These are intentionally separate from `entity`.
- `mapper/EspnGameMapper`, `mapper/EspnBoxScoreMapper` — ESPN model → `Game`/`GameStat` entities.
- `mapper/GameResponseMapper` — entity → `GameResponse` DTO. Controllers must return DTOs, never JPA entities directly.
- `service/EspnService` — builds ESPN URLs (`{base}/{sport}/{league}/scoreboard` and `.../summary?event={id}`) and fetches via `RestTemplate`. League/sport path segments come from the `League` enum.
- `service/GameServiceImpl` — persists fetched games and runs a `@Scheduled(fixedRate = 20000)` job (`updateGamesForToday`) that checks `GameRepository.shouldUpdate()` (any game live or due to start in the last 24h) before re-fetching ESPN data for every league.
- `service/CalendarServiceImpl` — computes "current" league context (year/seasonType/week/date) via a native query (`GameRepository.findCurrentContext`) that prioritizes in-progress games, then next upcoming, then most recent final. Result is cached in `currentContext` (key = league + timezone), evicted every 5 minutes (`CacheConfig`).
- `enums/League` — single source of truth for supported leagues (`nfl`, `college-football`, `nhl`, `usa.1`) and their capability flags (`supportsYearFilter`, `supportsWeekFilter`, `supportsStandings`, `contextMode` — `"season"` vs `"date"` — `statsProfile`, `teamOrder`, `supportsOdds`, `supportsLiveDetails`). Controllers and services branch on these flags instead of hardcoding per-league logic.
- `config/ApiKeyAuthFilter` + `config/SecurityConfig` — stateless `x-api-key` header check via a filter placed before Spring Security's auth filter; CORS is locked to `https://tallyo.us` (update `SecurityConfig` if the frontend origin changes).
- `exception/GlobalExceptionHandler` — all errors return the same `ApiError` envelope (`code`, `message`, `details?`, `path?`, `timestamp?`). `InvalidRequestException` → 400; `DateTimeException` → 400 `INVALID_TIMEZONE`; `RestClientException`/`ResourceAccessException` (ESPN upstream failures) → 502; everything else → 500.

### Timezone/date handling

Endpoints computing "current" context or date boundaries accept a `userTimeZone` param (default `America/New_York`). Never hardcode a timezone for returned date context. The current-context cache key includes both league and timezone, so different timezones don't collide.

### Adding a new league

1. Add an enum entry in `League` with correct sport/value/capability flags.
2. Confirm the ESPN scoreboard/summary URLs resolve for that league's sport/value.
3. Confirm `EspnGameMapper`/`EspnBoxScoreMapper` handle its sport-specific fields safely.
4. Confirm `/context`, `/dates`, `/games/current` produce sane data (contextMode `"season"` vs `"date"` matters here).
5. Update `openapi.yaml` if request/response shape changes, then regenerate frontend types in `../tallyo`.

## API contract discipline

`openapi.yaml` is maintained by hand and is the source of truth consumed by the frontend's generated types. Any change to endpoint behavior, params, or response shape must update `openapi.yaml` in the same change. CI (`openapi-check.yml`) validates the YAML parses and that the project compiles, but does not diff the contract against the code — keeping them in sync is manual.

## Deployment

Push to `main` triggers `.github/workflows/deploy.yml`, which SSHes into the prod host (via `cloudflared` tunnel) and runs `git pull && ./deploy.sh`, which rebuilds the jar and recreates the `docker-compose.yml` stack (`app` + `postgres` + `dozzle` log viewer). Hibernate runs with `ddl-auto: update` — schema changes apply automatically on boot, so be deliberate about entity changes.
