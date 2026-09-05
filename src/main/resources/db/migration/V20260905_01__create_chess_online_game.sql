CREATE TABLE chess_online_game (
    id BIGSERIAL PRIMARY KEY,
    game_id VARCHAR(36) NOT NULL,
    time_control VARCHAR(20) NOT NULL,
    white_member_id BIGINT NOT NULL,
    white_nickname VARCHAR(100) NOT NULL,
    black_member_id BIGINT NOT NULL,
    black_nickname VARCHAR(100) NOT NULL,
    moves TEXT NOT NULL DEFAULT '',
    status VARCHAR(40) NOT NULL,
    result VARCHAR(16),
    pgn TEXT NOT NULL DEFAULT '',
    white_millis BIGINT NOT NULL,
    black_millis BIGINT NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chess_online_game_game_id UNIQUE (game_id),
    CONSTRAINT fk_chess_online_game_white FOREIGN KEY (white_member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_chess_online_game_black FOREIGN KEY (black_member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_chess_online_game_white ON chess_online_game (white_member_id, finished_at DESC);
CREATE INDEX idx_chess_online_game_black ON chess_online_game (black_member_id, finished_at DESC);
