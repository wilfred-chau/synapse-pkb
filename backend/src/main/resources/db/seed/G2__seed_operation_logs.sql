INSERT INTO operation_logs (
    actor_user_id,
    actor_username,
    action,
    target_type,
    target_id,
    request_id,
    success,
    message,
    details_json,
    occurred_at
) VALUES (
    1,
    'pkb-admin',
    'SEED_OPERATION_LOG',
    'SYSTEM',
    'seed-validation',
    'seed-request-id',
    TRUE,
    'Seed operation log for validation',
    '{"source":"seed","note":"validate operation_logs table"}',
    CURRENT_TIMESTAMP
);
