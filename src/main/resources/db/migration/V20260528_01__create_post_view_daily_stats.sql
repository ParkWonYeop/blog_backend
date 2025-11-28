CREATE TABLE post_view_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_view_daily_stats_post
        FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT uk_post_view_daily_stats_post_date
        UNIQUE (post_id, stat_date)
);

CREATE INDEX idx_post_view_daily_stats_date
    ON post_view_daily_stats (stat_date);

CREATE INDEX idx_post_view_daily_stats_post_id
    ON post_view_daily_stats (post_id);
