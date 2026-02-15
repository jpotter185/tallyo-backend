# Tallyo Backend Specification (Initial)

## 1) Service Scope
- Spring Boot backend for `tallyo` frontend.
- Stores and serves normalized game/team data.
- Ingests ESPN scoreboard + stats data.
- Exposes game context and date discovery for league screens.

## 2) API Surface
Base path: `/api/v1/games`

Key endpoints:
- `GET /api/v1/games`
- `GET /api/v1/games/current`
- `GET /api/v1/games/context`
- `GET /api/v1/games/dates`
- `GET /api/v1/games/nhl-dates` (legacy/special-case)
- `POST /api/v1/games` (manual update trigger)

## 3) Contract Source of Truth
- OpenAPI file: `openapi.yaml`
- Frontend consumes this contract via generated TS types in sibling repo:
  - `../tallyo/src/types/api.generated.ts`

When endpoint request/response behavior changes:
1. Update code.
2. Update `openapi.yaml` in same PR.
3. Regenerate frontend types in `../tallyo`.

## 4) Response Model Rules
- Read endpoints should return DTOs, not JPA entities.
- Current game list endpoints return `PageResponse<GameResponse>`.
- Error responses must use stable envelope:
  - `code`
  - `message`
  - `details?`
  - `path?`
  - `timestamp?`

## 5) Error Handling
- Use `InvalidRequestException` for client input errors.
- Global mapping in `GlobalExceptionHandler` for:
  - invalid requests (400)
  - invalid timezone/date parsing (400)
  - unexpected server errors (500)
- API key auth failures return the same error envelope with 403.

## 6) Timezone Rules
- Endpoints that compute “current” context or date boundaries must accept `userTimeZone`.
- Context cache key must include both league and timezone.
- Never hardcode America/New_York for returned date context unless explicitly intended.

## 7) League Onboarding Checklist
1. Add enum entry in `League` (sport/value/capabilities).
2. Ensure ESPN fetch URLs work for scoreboard/summary.
3. Confirm mapping handles sport-specific fields/stats safely.
4. Ensure repository queries support league filtering.
5. Confirm `/context`, `/dates`, `/games/current` produce valid data for that league.
6. Update `openapi.yaml` if behavior/query params/schema changed.
7. Validate frontend type generation still succeeds.

## 8) Commands
- Compile: `./mvnw -q -DskipTests compile`
- Test: `./mvnw test`
- Run: `./mvnw spring-boot:run`

## 9) Current Constraints / Notes
- API key auth is mandatory for API routes.
- CORS currently configured for `https://tallyo.us`.
- OpenAPI currently maintained manually.

## 10) Planned Improvements
- Add OpenAPI validation in CI.
- Add integration tests for timezone and error envelope behavior.
- Deprecate legacy league-specific endpoints where generic equivalents exist.
