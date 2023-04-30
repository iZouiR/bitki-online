CREATE TABLE IF NOT EXISTS players
(
    id            BIGSERIAL PRIMARY KEY,
    chat_id       BIGINT UNIQUE NOT NULL,
    username      VARCHAR(128) UNIQUE,
    is_playing    BOOLEAN,
    rank          INT,
    registered_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eggs
(
    id           BIGSERIAL PRIMARY KEY,
    type         VARCHAR(32)  NOT NULL,
    name         VARCHAR(128) NOT NULL,
    is_cracked   BOOLEAN      NOT NULL,
    endurance    INT          NOT NULL,
    power        INT          NOT NULL,
    luck         INT          NOT NULL,
    intelligence INT          NOT NULL,
    image_path   VARCHAR(256) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    owner_id     BIGINT REFERENCES players (id)
);

CREATE TABLE IF NOT EXISTS player_battles
(
    id                     BIGSERIAL PRIMARY KEY,
    first_player_id        BIGINT REFERENCES players (id),
    second_player_id       BIGINT REFERENCES players (id),
    first_player_egg_id    BIGINT REFERENCES eggs (id),
    second_player_egg_id   BIGINT REFERENCES eggs (id),
    is_first_player_winner BOOLEAN
);

CREATE TABLE IF NOT EXISTS match_making_battles
(
    id               BIGSERIAL PRIMARY KEY,
    player_battle_id BIGINT REFERENCES player_battles (id)
);

CREATE TABLE IF NOT EXISTS private_battles
(
    id               BIGSERIAL PRIMARY KEY,
    player_battle_id BIGINT REFERENCES player_battles (id),
    link             VARCHAR(128) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS player_bots
(
    id                   BIGSERIAL PRIMARY KEY,
    player_id            BIGINT REFERENCES players (id),
    last_state           VARCHAR(128),
    last_inventory_index INT,
    last_inventory_size  INT
)