CREATE TABLE IF NOT EXISTS players
(
    id            BIGSERIAL PRIMARY KEY,
    chat_id       BIGINT UNIQUE       NOT NULL,
    username      VARCHAR(128) UNIQUE NOT NULL,
    rank          BIGINT              NOT NULL,
    registered_at TIMESTAMP           NOT NULL
);

CREATE TABLE IF NOT EXISTS eggs
(
    id           BIGSERIAL PRIMARY KEY,
    type         VARCHAR(32)  NOT NULL,
    endurance    BIGINT       NOT NULL,
    luck         BIGINT       NOT NULL,
    intelligence BIGINT       NOT NULL,
    image_path   VARCHAR(256) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    owner_id     BIGINT REFERENCES players (id)
);

CREATE TABLE IF NOT EXISTS match_making_battles
(
    id                     BIGSERIAL PRIMARY KEY,
    first_player_id        BIGINT REFERENCES players (id),
    second_player_id       BIGINT REFERENCES players (id),
    first_player_egg_id    BIGINT REFERENCES eggs (id),
    second_player_egg_id   BIGINT REFERENCES eggs (id),
    is_first_player_winner BOOLEAN
);

CREATE TABLE IF NOT EXISTS private_battles
(
    id                     BIGSERIAL PRIMARY KEY,
    link                   VARCHAR(128) UNIQUE NOT NULL,
    first_player_id        BIGINT REFERENCES players (id),
    second_player_id       BIGINT REFERENCES players (id),
    first_player_egg_id    BIGINT REFERENCES eggs (id),
    second_player_egg_id   BIGINT REFERENCES eggs (id),
    is_first_player_winner BOOLEAN
);