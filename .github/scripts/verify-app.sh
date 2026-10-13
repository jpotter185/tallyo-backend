#!/usr/bin/env bash
# Start, inspect and stop the app for end-to-end verification in CI (used by claude-fix.yml).
# Expects Postgres on localhost:5432 with user tallyo (the workflow's service container). On a Mac, follow the
# verify skill instead.
#   verify-app.sh start   build + boot, wait for /actuator/health UP and the startup schedule refresh, then apply
#                         db/migrations with Liquibase (prod runs them on deploy; see db/README.md)
#   verify-app.sh get /path?query    GET localhost:8080/path with the verify API key; prints the body, then the status
#   verify-app.sh post /path?query   same with POST
#   verify-app.sh logs    last 200 lines of the app log
#   verify-app.sh stop    stop the app
set -uo pipefail
LOG=/tmp/tallyo-app.log
PIDFILE=/tmp/tallyo-app.pid

# Same Liquibase image as the compose `migrate` service. Runs after the app so Hibernate has created the schema,
# which is the state prod is in when deploy.sh migrates.
migrate() {
  local image
  image=$(grep -o 'liquibase/liquibase:[^ ]*' docker-compose.yml)
  echo "applying migrations with $image"
  docker run --rm --network host -v "$PWD/db:/liquibase/changelog:ro" \
    -e LIQUIBASE_COMMAND_URL=jdbc:postgresql://localhost:5432/postgres \
    -e LIQUIBASE_COMMAND_USERNAME=tallyo \
    -e LIQUIBASE_COMMAND_CHANGELOG_FILE=changelog.yaml \
    -e LIQUIBASE_SEARCH_PATH=/liquibase/changelog \
    -e LIQUIBASE_ANALYTICS_ENABLED=false \
    "$image" update 2>&1 | tail -40
  local status=${PIPESTATUS[0]}
  [ "$status" -eq 0 ] && echo "migrations applied" || echo "MIGRATIONS FAILED (exit $status)"
  return "$status"
}

case "${1:-}" in
  start)
    API_KEY=verify-key SPRING_DATASOURCE_USERNAME=tallyo setsid nohup ./mvnw -q spring-boot:run >"$LOG" 2>&1 &
    echo $! >"$PIDFILE"
    for _ in $(seq 1 150); do  # up to 5 min: build, boot, then the startup ESPN schedule refresh
      if grep -q "Finished refreshing schedules" "$LOG" 2>/dev/null; then
        echo "app is UP and the schedule refresh finished"; migrate; exit $?
      fi
      if ! kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
        echo "app exited during startup; last log lines:"; tail -80 "$LOG"; exit 1
      fi
      sleep 2
    done
    if curl -sf localhost:8080/actuator/health >/dev/null; then
      echo "app is UP but the schedule refresh has not finished (data may be partial)"; migrate; exit $?
    fi
    echo "app did not become healthy; last log lines:"; tail -80 "$LOG"; exit 1
    ;;
  get|post)
    # Builds the URL itself (always localhost, always the verify key), so the workflow's tool allowlist needs no curl patterns.
    path=${2:-}
    case "$path" in
      /*) ;;
      *) echo "usage: $0 $1 /path?query  (path must start with /; URL-encode spaces)"; exit 2 ;;
    esac
    if [ "$#" -ne 2 ] || printf '%s' "$path" | grep -q '[[:space:]]'; then
      echo "usage: $0 $1 /path?query  (one argument, no spaces)"; exit 2
    fi
    method=GET; [ "$1" = post ] && method=POST
    curl -s -X "$method" -H 'x-api-key: verify-key' -w '\nHTTP %{http_code}\n' "http://localhost:8080$path"
    ;;
  logs)
    tail -200 "$LOG"
    ;;
  stop)
    [ -f "$PIDFILE" ] && kill -TERM -- -"$(cat "$PIDFILE")" 2>/dev/null
    rm -f "$PIDFILE"; echo "stopped"
    ;;
  *)
    echo "usage: $0 start|get|post|logs|stop"; exit 2
    ;;
esac
