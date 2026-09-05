INSERT INTO pkb_users (
    username,
    password_hash,
    display_name,
    space_key,
    enabled
) VALUES (
    'pkb-admin',
    '$2a$10$OvImYaRHJcjHfwB5x1WrSOu0SIAMVIpp2Au5RBKqpjQblaRI6.2MS',
    'Wilfred',
    'personal-space',
    TRUE
)
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    display_name = EXCLUDED.display_name,
    space_key = EXCLUDED.space_key,
    enabled = EXCLUDED.enabled,
    updated_at = CURRENT_TIMESTAMP;
