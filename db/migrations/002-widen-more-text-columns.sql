--liquibase formatted sql

-- Issue #12: varchar overflow persisted after 001 in another free-text column. Widen the remaining ESPN-sourced
-- text columns so no single long value can fail a whole league update. IF EXISTS keeps this a no-op on a fresh
-- database, where Hibernate creates the columns itself.
--changeset tallyo:002-widen-more-text-columns
ALTER TABLE IF EXISTS games
  ALTER COLUMN stadium_name TYPE text,
  ALTER COLUMN location TYPE text,
  ALTER COLUMN channel TYPE text,
  ALTER COLUMN espn_link TYPE text;
ALTER TABLE IF EXISTS game_players
  ALTER COLUMN stat_labels TYPE text,
  ALTER COLUMN stat_values TYPE text;
ALTER TABLE IF EXISTS scoring_plays
  ALTER COLUMN display_text TYPE text;
ALTER TABLE IF EXISTS game_stats
  ALTER COLUMN stat_value TYPE text;
ALTER TABLE IF EXISTS game_leaders
  ALTER COLUMN display_value TYPE text;
