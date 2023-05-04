CREATE TABLE IF NOT EXISTS support_messages
(
    id      BIGSERIAL PRIMARY KEY,
    chat_id BIGINT REFERENCES players (chat_id),
    message VARCHAR(512) NOT NULL,
    sent_at TIMESTAMP    NOT NULL
);