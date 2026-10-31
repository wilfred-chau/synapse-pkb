-- UTF8 PostgreSQL 16. Keep whitespace and NFC rules in sync with TagName.
CREATE FUNCTION pkb_tag_normalize_v1(value TEXT)
RETURNS TEXT
LANGUAGE SQL IMMUTABLE STRICT PARALLEL SAFE
RETURN btrim(regexp_replace(
    translate(normalize(value, NFC),
        U&'\0009\000A\000B\000C\000D\0020\0085\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000',
        repeat(' ', 25)),
    ' +', ' ', 'g'));

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES pkb_users(id),
    name VARCHAR(32) COLLATE "C" NOT NULL,
    name_key TEXT COLLATE "C" GENERATED ALWAYS AS
        (translate(name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) STORED NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tags_name CHECK (
        char_length(name) BETWEEN 1 AND 32
        AND name = pkb_tag_normalize_v1(name)
        AND name !~ U&'[\0001-\001F\007F-\009F\200B\FEFF]'
    ),
    CONSTRAINT uk_tags_user_name_key UNIQUE (user_id, name_key)
);
