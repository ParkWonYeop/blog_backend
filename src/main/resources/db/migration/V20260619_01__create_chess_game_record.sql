CREATE TABLE chess_game_record (
    id BIGSERIAL PRIMARY KEY,
    game_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    player_color VARCHAR(10) NOT NULL,
    model VARCHAR(10) NOT NULL,
    temperature DOUBLE PRECISION NOT NULL,
    top_p DOUBLE PRECISION NOT NULL,
    fen TEXT NOT NULL,
    turn VARCHAR(10) NOT NULL,
    moves TEXT NOT NULL DEFAULT '',
    status VARCHAR(40) NOT NULL,
    result VARCHAR(16),
    outcome VARCHAR(20) NOT NULL,
    pgn TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chess_game_record_game_id UNIQUE (game_id),
    CONSTRAINT fk_chess_game_record_member
        FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_chess_game_record_member_updated_at
    ON chess_game_record (member_id, updated_at DESC);

CREATE INDEX idx_chess_game_record_member_outcome
    ON chess_game_record (member_id, outcome);
