-- 1. UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Ajustes de la partida
CREATE TABLE settings (
  settings_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rounds               INTEGER NOT NULL CHECK (rounds > 0),
  time_per_round       INTEGER NOT NULL CHECK (time_per_round > 0),
  questions_per_round  INTEGER NOT NULL CHECK (questions_per_round > 0),
  difficulty           TEXT    NOT NULL CHECK (difficulty IN ('easy','medium','hard')),
  max_players_per_team INTEGER NOT NULL CHECK (max_players_per_team > 0)
);

-- 3. Salas / rooms
CREATE TABLE rooms (
  room_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug        TEXT UNIQUE NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  settings_id UUID NOT NULL REFERENCES settings(settings_id) ON DELETE CASCADE
);

-- 4. Equipos / teams
CREATE TABLE teams (
  team_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id UUID NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
  name    TEXT NOT NULL
);

-- 5. Jugadores / players
CREATE TABLE players (
  player_id  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id    UUID    NOT NULL REFERENCES rooms(room_id)    ON DELETE CASCADE,
  username   TEXT    NOT NULL,
  is_host    BOOLEAN NOT NULL DEFAULT FALSE,
  team_id    UUID    NULL REFERENCES teams(team_id)         ON DELETE SET NULL
);

-- 6. Partidas / games
CREATE TABLE games (
  game_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id    UUID NOT NULL REFERENCES rooms(room_id)        ON DELETE CASCADE,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at   TIMESTAMPTZ NULL
);

-- 7. Rondas / rounds
CREATE TABLE rounds (
  round_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  game_id    UUID NOT NULL REFERENCES games(game_id)        ON DELETE CASCADE,
  number     INTEGER NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at   TIMESTAMPTZ NULL,
  CONSTRAINT unique_round_number UNIQUE (game_id, number)
);

-- 8. Preguntas / questions
CREATE TABLE questions (
  question_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  round_id     UUID NOT NULL REFERENCES rounds(round_id)    ON DELETE CASCADE,
  type         TEXT NOT NULL CHECK (type IN ('multiple_choice','short_answer','buzzer')),
  text         TEXT NOT NULL,
  media_url    TEXT NULL
);

-- 9. Opciones de elección múltiple
CREATE TABLE question_options (
  option_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id UUID NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
  text        TEXT NOT NULL,
  is_correct  BOOLEAN NOT NULL DEFAULT FALSE
);

-- 10. Respuestas / responses
CREATE TABLE responses (
  response_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id  UUID NOT NULL REFERENCES questions(question_id)      ON DELETE CASCADE,
  player_id    UUID NOT NULL REFERENCES players(player_id)          ON DELETE CASCADE,
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  text_reply   TEXT NULL,
  option_id    UUID NULL REFERENCES question_options(option_id)     ON DELETE SET NULL,
  is_correct   BOOLEAN NULL
);

-- 11. Puntuaciones de equipo por ronda
CREATE TABLE team_scores (
  score_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  round_id UUID NOT NULL REFERENCES rounds(round_id)     ON DELETE CASCADE,
  team_id  UUID NOT NULL REFERENCES teams(team_id)       ON DELETE CASCADE,
  points   INTEGER NOT NULL DEFAULT 0
);

-- 12. Índices de apoyo
CREATE INDEX ON rooms(slug);
CREATE INDEX ON players(room_id);
CREATE INDEX ON players(team_id);
CREATE INDEX ON teams(room_id);
CREATE INDEX ON games(room_id);
CREATE INDEX ON rounds(game_id);
CREATE INDEX ON questions(round_id);
CREATE INDEX ON question_options(question_id);
CREATE INDEX ON responses(question_id);
CREATE INDEX ON responses(player_id);
CREATE INDEX ON team_scores(round_id);
CREATE INDEX ON team_scores(team_id);

-- 13. Un único host por sala
CREATE UNIQUE INDEX one_host_per_room
  ON players(room_id)
  WHERE is_host;

-- 14. Un único juego activo por sala (ended_at IS NULL)
CREATE UNIQUE INDEX one_active_game_per_room
  ON games(room_id)
  WHERE ended_at IS NULL;

-- 15. Trigger para asegurar máximo de jugadores por equipo
-- 15.1. Función plpgsql
CREATE OR REPLACE FUNCTION enforce_max_players_per_team()
RETURNS trigger AS $$
DECLARE
  max_allowed   INTEGER;
  current_count INTEGER;
BEGIN
  -- Si no hay equipo asignado, no hacemos nada
  IF NEW.team_id IS NULL THEN
    RETURN NEW;
  END IF;

  -- Obtener el límite de jugadores por equipo desde settings
  SELECT s.max_players_per_team
    INTO max_allowed
    FROM rooms r
    JOIN settings s ON r.settings_id = s.settings_id
    WHERE r.room_id = NEW.room_id;

  -- Contar cuántos jugadores ya están en ese equipo (excluyendo el propio registro en UPDATE)
  SELECT COUNT(*)
    INTO current_count
    FROM players
    WHERE team_id = NEW.team_id
      AND (TG_OP = 'INSERT' OR player_id <> NEW.player_id);

  IF current_count >= max_allowed THEN
    RAISE EXCEPTION 'El equipo % ya tiene el número máximo de jugadores (%).', NEW.team_id, max_allowed;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 15.2. Trigger sobre players
CREATE TRIGGER trg_enforce_max_players_per_team
BEFORE INSERT OR UPDATE ON players
FOR EACH ROW
WHEN (NEW.team_id IS NOT NULL)
EXECUTE PROCEDURE enforce_max_players_per_team();
