CREATE TABLE IF NOT EXISTS player_bots
(
    id                   BIGSERIAL PRIMARY KEY,
    player_id            BIGINT REFERENCES players (id),
    last_bot_state       VARCHAR(128),
    last_inventory_index INT,
    last_inventory_size  INT
)