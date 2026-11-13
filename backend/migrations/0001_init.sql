CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device      TEXT UNIQUE,
    email       TEXT UNIQUE,
    pass_hash   TEXT,
    role        TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'editor', 'admin')),
    banned      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan        TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX subscriptions_user ON subscriptions(user_id, expires_at);

CREATE TABLE categories (
    id        TEXT PRIMARY KEY,
    kind      TEXT NOT NULL CHECK (kind IN ('movie', 'series', 'channel')),
    name      TEXT NOT NULL,
    position  INT NOT NULL DEFAULT 0
);
CREATE INDEX categories_kind ON categories(kind, position);

CREATE TABLE items (
    id            TEXT PRIMARY KEY,
    kind          TEXT NOT NULL CHECK (kind IN ('movie', 'series', 'channel')),
    title         TEXT NOT NULL,
    description   TEXT,
    poster        TEXT,
    backdrop      TEXT,
    logo          TEXT,
    year          INT,
    duration_min  INT,
    quality       TEXT,
    category_id   TEXT REFERENCES categories(id) ON DELETE SET NULL,
    live          BOOLEAN NOT NULL DEFAULT FALSE,
    online        BOOLEAN NOT NULL DEFAULT TRUE,
    featured      BOOLEAN NOT NULL DEFAULT FALSE,
    premium       BOOLEAN NOT NULL DEFAULT FALSE,
    published     BOOLEAN NOT NULL DEFAULT TRUE,
    position      INT NOT NULL DEFAULT 0,
    stream_url    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX items_kind_pos ON items(kind, published, position, created_at DESC);
CREATE INDEX items_category ON items(category_id);
CREATE INDEX items_title_trgm ON items USING gin (title gin_trgm_ops);

CREATE TABLE seasons (
    id         TEXT PRIMARY KEY,
    series_id  TEXT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    number     INT NOT NULL,
    title      TEXT NOT NULL DEFAULT ''
);
CREATE INDEX seasons_series ON seasons(series_id, number);

CREATE TABLE episodes (
    id            TEXT PRIMARY KEY,
    season_id     TEXT NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
    number        INT NOT NULL,
    title         TEXT NOT NULL DEFAULT '',
    duration_min  INT,
    poster        TEXT,
    stream_url    TEXT
);
CREATE INDEX episodes_season ON episodes(season_id, number);
CREATE INDEX episodes_title_trgm ON episodes USING gin (title gin_trgm_ops);

-- المصدر: featured | latest_movies | latest_series | channels_featured | recent | recommended | categories | continue
CREATE TABLE home_sections (
    id        TEXT PRIMARY KEY,
    type      TEXT NOT NULL CHECK (type IN ('hero', 'row', 'channels', 'continue', 'categories')),
    title     TEXT NOT NULL DEFAULT '',
    source    TEXT NOT NULL,
    position  INT NOT NULL DEFAULT 0,
    enabled   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE app_config (
    key    TEXT PRIMARY KEY,
    value  JSONB NOT NULL
);

CREATE TABLE favorites (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id  TEXT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, item_id)
);

CREATE TABLE history (
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key          TEXT NOT NULL,
    item_id      TEXT NOT NULL,
    episode_id   TEXT,
    position_ms  BIGINT NOT NULL DEFAULT 0,
    duration_ms  BIGINT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, key)
);

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT NOT NULL,
    body        TEXT NOT NULL,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ads (
    id         TEXT PRIMARY KEY,
    placement  TEXT NOT NULL,
    image      TEXT,
    target     TEXT,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    position   INT NOT NULL DEFAULT 0
);

CREATE TABLE view_stats (
    item_id  TEXT NOT NULL,
    day      DATE NOT NULL DEFAULT CURRENT_DATE,
    plays    BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (item_id, day)
);

INSERT INTO app_config(key, value) VALUES
    ('app', '{"minVersion": 0, "maxReconnect": 6, "maintenance": null}');

INSERT INTO home_sections(id, type, title, source, position) VALUES
    ('hero',     'hero',       '',                  'featured',          0),
    ('continue', 'continue',   'متابعة المشاهدة',   'continue',          1),
    ('channels', 'channels',   'القنوات المميزة',   'channels_featured', 2),
    ('movies',   'row',        'أحدث الأفلام',      'latest_movies',     3),
    ('series',   'row',        'أحدث المسلسلات',    'latest_series',     4),
    ('recent',   'row',        'أضيف حديثاً',       'recent',            5),
    ('cats',     'categories', 'التصنيفات',         'categories',        6),
    ('rec',      'row',        'مقترح لك',          'recommended',       7);
