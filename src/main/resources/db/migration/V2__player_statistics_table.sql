CREATE TABLE IF NOT EXISTS player_statistics
(
    id                          BIGSERIAL PRIMARY KEY,
    player_id                   BIGINT REFERENCES players (id),
    total_damage_dealt          BIGINT,
    total_damage_taken          BIGINT,
    total_rank_points_earned    BIGINT,
    total_rank_points_lost      BIGINT,
    head_attack_chosen          BIGINT,
    side_attack_chosen          BIGINT,
    ass_attack_chosen           BIGINT,
    head_attack_succeed         BIGINT,
    side_attack_succeed         BIGINT,
    ass_attack_succeed          BIGINT,
    total_battles_played        BIGINT,
    total_battles_won           BIGINT,
    total_eggs_obtained         BIGINT,
    holy_eggs_obtained          BIGINT,
    strong_eggs_obtained        BIGINT,
    weak_eggs_obtained          BIGINT
);