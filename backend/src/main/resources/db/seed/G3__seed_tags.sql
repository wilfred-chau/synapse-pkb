-- Run with psql on CentOS after V3, ONLY in the isolated A2 test database.
-- Disabled fixture users cannot log in. HTTP tests create their own enabled users.
\set ON_ERROR_STOP on
SELECT current_database() = 'synapse_pkb_a2_test' AS is_test_database \gset
\if :is_test_database
BEGIN;
INSERT INTO pkb_users(username, password_hash, display_name, space_key, enabled)
VALUES ('a2-seed-one', '!disabled!', 'A2 Seed One', 'a2-seed-one', false),
       ('a2-seed-two', '!disabled!', 'A2 Seed Two', 'a2-seed-two', false)
ON CONFLICT (username) DO NOTHING;

INSERT INTO tags(user_id, name)
SELECT u.id, names.name
FROM pkb_users u
CROSS JOIN (VALUES (U&'\673A\5668\5B66\4E60'), (U&'\6DF1\5EA6\5B66\4E60'),
                   (U&'\79D1\5E7B'), (U&'\9605\8BFB\7B14\8BB0'), ('Java')) AS names(name)
WHERE u.username IN ('a2-seed-one', 'a2-seed-two')
ON CONFLICT (user_id, name_key) DO NOTHING;
COMMIT;
SELECT u.username, t.name FROM tags t JOIN pkb_users u ON u.id = t.user_id
WHERE u.username IN ('a2-seed-one', 'a2-seed-two') ORDER BY u.username, t.name_key;
\else
\echo 'Refusing to seed outside synapse_pkb_a2_test'
\quit 1
\endif
