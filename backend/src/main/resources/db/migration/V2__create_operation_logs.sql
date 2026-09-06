CREATE TABLE operation_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_username VARCHAR(100),
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128),
    request_id VARCHAR(64),
    success BOOLEAN NOT NULL,
    message VARCHAR(255),
    details_json TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operation_logs_actor_user_id ON operation_logs (actor_user_id);
CREATE INDEX idx_operation_logs_action ON operation_logs (action);
CREATE INDEX idx_operation_logs_target ON operation_logs (target_type, target_id);
CREATE INDEX idx_operation_logs_occurred_at ON operation_logs (occurred_at DESC);
