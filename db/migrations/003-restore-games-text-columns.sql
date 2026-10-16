--liquibase formatted sql

-- Issue #14: 001 was applied, then Hibernate's ddl-auto: update narrowed these columns back to varchar(255) when the
-- app started with an entity that lacked columnDefinition = "text" (Hibernate narrows varchar-vs-text, never widens).
-- Re-apply the widening. IF EXISTS keeps this a no-op on a fresh database, where Hibernate creates the columns itself.
--changeset tallyo:003-restore-games-text-columns
ALTER TABLE IF EXISTS games
  ALTER COLUMN last_play TYPE text,
  ALTER COLUMN current_down_and_distance TYPE text,
  ALTER COLUMN headline TYPE text;
