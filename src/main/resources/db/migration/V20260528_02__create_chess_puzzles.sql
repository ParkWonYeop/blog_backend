CREATE TABLE chess_puzzle (
    id BIGSERIAL PRIMARY KEY,
    source_puzzle_id VARCHAR(32) NOT NULL,
    source_url VARCHAR(255) NOT NULL,
    title VARCHAR(100) NOT NULL,
    theme VARCHAR(80) NOT NULL,
    fen VARCHAR(120) NOT NULL,
    answer VARCHAR(20) NOT NULL,
    answer_uci VARCHAR(8) NOT NULL,
    hint VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL,
    popularity INTEGER NOT NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chess_puzzle_source_puzzle_id UNIQUE (source_puzzle_id)
);

CREATE INDEX idx_chess_puzzle_active_sort_order
    ON chess_puzzle (active, sort_order);

INSERT INTO chess_puzzle (
    source_puzzle_id,
    source_url,
    title,
    theme,
    fen,
    answer,
    answer_uci,
    hint,
    rating,
    popularity,
