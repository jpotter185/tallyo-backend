---
name: verify
description: Build, launch, and drive the Tallyo backend locally to verify changes end-to-end against a throwaway Postgres.
---

# Verify Tallyo backend changes

## Database (no local Postgres service runs by default)

Homebrew Postgres 15 is installed at `/opt/homebrew/opt/postgresql@15/bin`.
Spin up a throwaway cluster in the session scratchpad (never reuse a real data dir):

```bash
export PATH=/opt/homebrew/opt/postgresql@15/bin:$PATH
initdb -D "$SCRATCH/pgdata" -U tallyo --auth=trust -E UTF8
# Unix socket paths under the scratchpad exceed the 103-byte limit — TCP only:
pg_ctl -D "$SCRATCH/pgdata" -l "$SCRATCH/pgdata/pg.log" -o "-p 5432 -c unix_socket_directories=''" start
```

Hibernate `ddl-auto: update` creates the schema on boot; `LeagueConstraintSync`
refreshes the league check constraints.

## Launch

```bash
API_KEY=verify-key SPRING_DATASOURCE_USERNAME=tallyo ./mvnw -q spring-boot:run
```

Run in background; ready in ~15s. On startup it fetches the full current-year
schedule for every league from ESPN (real network calls), so games appear
without any manual step.

## Drive

Every request needs `-H "x-api-key: verify-key"`. Useful flows:

```bash
curl -s localhost:8080/api/v1/leagues -H "x-api-key: verify-key"
curl -s "localhost:8080/api/v1/games/current?league=<id>" -H ...
curl -s "localhost:8080/api/v1/games/context?league=<id>&userTimeZone=Asia/Tokyo" -H ...
curl -s "localhost:8080/api/v1/standings?league=<id>" -H ...
curl -s "localhost:8080/api/v1/games/<gameId>/details" -H ...
# Backfill stats/leaders/scoring plays (schedule refresh saves none):
curl -s -X POST "localhost:8080/api/v1/games?league=<id>&shouldFetchStats=true" -H ...
```

## Gotchas

- Read scores from top-level `homeScore`/`awayScore` on GameResponse. The
  nested `team.score` is a shared Team-entity field and is stale by design.
- Standings group name field is `groupName`, not `name`.
- `/games/{id}/details` serves only stored data; games ingested by the
  schedule-only refresh have no leaders/scoringPlays until a stats fetch
  (20s live job or the POST backfill) touches them.
- League query param is the lowercase enum name (`world_cup`, not `world-cup`);
  matching is case-insensitive via `valueOf(param.toUpperCase())`.

## Teardown

Kill the spring-boot background task, then
`pg_ctl -D "$SCRATCH/pgdata" stop`.
