--liquibase formatted sql

-- ESPN play text and headlines can exceed varchar(255) (issue #4). Hibernate's ddl-auto: update never widens an
-- existing column. IF EXISTS keeps this a no-op on a fresh database, where Hibernate creates the columns itself.
--changeset tallyo:001-widen-games-text-columns
ALTER TABLE IF EXISTS games
  ALTER COLUMN last_play TYPE text,
  ALTER COLUMN current_down_and_distance TYPE text,
  ALTER COLUMN headline TYPE text;
