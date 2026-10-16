--liquibase formatted sql

-- Issue #14: in prod the three columns widened by 001 were varchar(255) again although 001 is recorded as executed
-- (cause unknown). 001 can't be edited (checksum), so re-apply it. Widening an already-text column is a no-op.
-- IF EXISTS keeps this a no-op on a fresh database, where Hibernate creates the columns itself.
--changeset tallyo:003-reapply-games-text-columns
ALTER TABLE IF EXISTS games
  ALTER COLUMN last_play TYPE text,
  ALTER COLUMN current_down_and_distance TYPE text,
  ALTER COLUMN headline TYPE text;
