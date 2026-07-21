CREATE TABLE IF NOT EXISTS user_actions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    rating DOUBLE PRECISION NOT NULL,

    last_interaction_at
    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_user_actions
    PRIMARY KEY (user_id, event_id),

    CONSTRAINT chk_user_action_rating
    CHECK (rating >= 0.0 AND rating <= 1.0)
);

CREATE INDEX IF NOT EXISTS
    idx_user_actions_user_time
    ON user_actions (
    user_id,
    last_interaction_at DESC
);

CREATE INDEX IF NOT EXISTS
    idx_user_actions_event
    ON user_actions (event_id);


CREATE TABLE IF NOT EXISTS event_similarities (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,

    score DOUBLE PRECISION NOT NULL,

    updated_at
    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_event_similarities
    PRIMARY KEY (event_a, event_b),

    CONSTRAINT chk_ordered_event_pair
    CHECK (event_a < event_b),

    CONSTRAINT chk_similarity_score
    CHECK (score >= 0.0 AND score <= 1.0)
);

CREATE INDEX IF NOT EXISTS
    idx_event_similarities_event_a
    ON event_similarities (event_a);

CREATE INDEX IF NOT EXISTS
    idx_event_similarities_event_b
    ON event_similarities (event_b);

CREATE INDEX IF NOT EXISTS
    idx_event_similarities_score
    ON event_similarities (score DESC);