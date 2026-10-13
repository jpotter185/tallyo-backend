# Database migrations

Hibernate (`ddl-auto: update`) creates new tables and columns. It never widens, retypes, renames or drops existing
ones, and it doesn't fix data. Those changes go here, as Liquibase formatted-SQL files in `migrations/`.

`deploy.sh` runs `docker compose run --rm migrate` on every deploy, after the build and before the app is restarted.
Liquibase records each applied changeset in the `databasechangelog` table, so a file runs once per database. If a
migration fails, the deploy stops and the currently running app is left up.

## Adding a migration

Create `migrations/NNN-short-description.sql` with the next number (files run in name order):

```sql
--liquibase formatted sql

--changeset tallyo:NNN-short-description
ALTER TABLE IF EXISTS games ALTER COLUMN some_column TYPE text;
```

- Guard with `IF EXISTS`. On a brand-new database the migrations run before Hibernate has created any tables.
- The previous app version is still running while the migration applies, so keep changes compatible with it
  (widening a column is; renaming or dropping one it still uses is not).
- Never edit a file after it has been deployed. Liquibase checksums applied changesets and fails the deploy if one
  changes. Add a new file instead.
- In CI, `.github/scripts/verify-app.sh start` applies the migrations after the app has created the schema.
