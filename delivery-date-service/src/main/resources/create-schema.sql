-- CREATE TABLE IF NOT EXISTS public.event_journal
-- (
--     ordering           BIGSERIAL,
--     persistence_id     VARCHAR(255)          NOT NULL,
--     sequence_number    BIGINT                NOT NULL,
--     deleted            BOOLEAN DEFAULT FALSE NOT NULL,
--
--     writer             VARCHAR(255)          NOT NULL,
--     write_timestamp    BIGINT,
--     adapter_manifest   VARCHAR(255),
--
--     event_ser_id       INTEGER               NOT NULL,
--     event_ser_manifest VARCHAR(255)          NOT NULL,
--     event_payload      BYTEA                 NOT NULL,
--
--     meta_ser_id        INTEGER,
--     meta_ser_manifest  VARCHAR(255),
--     meta_payload       BYTEA,
--
--     PRIMARY KEY (persistence_id, sequence_number)
-- );
--
-- CREATE UNIQUE INDEX event_journal_ordering_idx ON public.event_journal (ordering);
--
-- CREATE TABLE IF NOT EXISTS public.event_tag
-- (
--     event_id BIGINT,
--     tag      VARCHAR(256),
--     PRIMARY KEY (event_id, tag),
--     CONSTRAINT fk_event_journal
--         FOREIGN KEY (event_id)
--             REFERENCES event_journal (ordering)
--             ON DELETE CASCADE
-- );
-- --
-- -- CREATE TABLE IF NOT EXISTS public.snapshot
-- -- (
-- --     persistence_id        VARCHAR(255) NOT NULL,
-- --     sequence_number       BIGINT       NOT NULL,
-- --     created               BIGINT       NOT NULL,
-- --
-- --     snapshot_ser_id       INTEGER      NOT NULL,
-- --     snapshot_ser_manifest VARCHAR(255) NOT NULL,
-- --     snapshot_payload      BYTEA        NOT NULL,
-- --
-- --     meta_ser_id           INTEGER,
-- --     meta_ser_manifest     VARCHAR(255),
-- --     meta_payload          BYTEA,
-- --
-- --     PRIMARY KEY (persistence_id, sequence_number)
-- -- );
--
-- CREATE TABLE IF NOT EXISTS public.durable_state
-- (
--     global_offset         BIGSERIAL,
--     persistence_id        VARCHAR(255) NOT NULL,
--     revision              BIGINT       NOT NULL,
--     state_payload         BYTEA        NOT NULL,
--     state_serial_id       INTEGER      NOT NULL,
--     state_serial_manifest VARCHAR(255),
--     tag                   VARCHAR,
--     state_timestamp       BIGINT       NOT NULL,
--     PRIMARY KEY (persistence_id)
-- );
-- CREATE INDEX CONCURRENTLY state_tag_idx on public.durable_state (tag);
-- CREATE INDEX CONCURRENTLY state_global_offset_idx on public.durable_state (global_offset);


CREATE TABLE IF NOT EXISTS event_journal
(
    slice              INT                      NOT NULL,
    entity_type        VARCHAR(255)             NOT NULL,
    persistence_id     VARCHAR(255)             NOT NULL,
    seq_nr             BIGINT                   NOT NULL,
    db_timestamp       timestamp with time zone NOT NULL,

    event_ser_id       INTEGER                  NOT NULL,
    event_ser_manifest VARCHAR(255)             NOT NULL,
    event_payload      BYTEA                    NOT NULL,

    deleted            BOOLEAN DEFAULT FALSE    NOT NULL,
    writer             VARCHAR(255)             NOT NULL,
    adapter_manifest   VARCHAR(255),
    tags               TEXT ARRAY,

    meta_ser_id        INTEGER,
    meta_ser_manifest  VARCHAR(255),
    meta_payload       BYTEA,

    PRIMARY KEY (persistence_id, seq_nr)
);

-- `event_journal_slice_idx` is only needed if the slice based queries are used
CREATE INDEX IF NOT EXISTS event_journal_slice_idx ON event_journal (slice, entity_type, db_timestamp, seq_nr);

CREATE TABLE IF NOT EXISTS snapshot
(
    slice             INT          NOT NULL,
    entity_type       VARCHAR(255) NOT NULL,
    persistence_id    VARCHAR(255) NOT NULL,
    seq_nr            BIGINT       NOT NULL,
    db_timestamp      timestamp with time zone,
    write_timestamp   BIGINT       NOT NULL,
    ser_id            INTEGER      NOT NULL,
    ser_manifest      VARCHAR(255) NOT NULL,
    snapshot          BYTEA        NOT NULL,
    tags              TEXT ARRAY,
    meta_ser_id       INTEGER,
    meta_ser_manifest VARCHAR(255),
    meta_payload      BYTEA,

    PRIMARY KEY (persistence_id)
);

-- `snapshot_slice_idx` is only needed if the slice based queries are used together with snapshot as starting point
CREATE INDEX IF NOT EXISTS snapshot_slice_idx ON snapshot (slice, entity_type, db_timestamp);

CREATE TABLE IF NOT EXISTS durable_state
(
    slice              INT                      NOT NULL,
    entity_type        VARCHAR(255)             NOT NULL,
    persistence_id     VARCHAR(255)             NOT NULL,
    revision           BIGINT                   NOT NULL,
    db_timestamp       timestamp with time zone NOT NULL,

    state_ser_id       INTEGER                  NOT NULL,
    state_ser_manifest VARCHAR(255),
    state_payload      BYTEA                    NOT NULL,
    tags               TEXT ARRAY,

    PRIMARY KEY (persistence_id, revision)
);

-- `durable_state_slice_idx` is only needed if the slice based queries are used
CREATE INDEX IF NOT EXISTS durable_state_slice_idx ON durable_state (slice, entity_type, db_timestamp, revision);


CREATE TABLE IF NOT EXISTS akka_projection_offset_store
(
    projection_name VARCHAR(255) NOT NULL,
    projection_key  VARCHAR(255) NOT NULL,
    current_offset  VARCHAR(255) NOT NULL,
    manifest        VARCHAR(4)   NOT NULL,
    mergeable       BOOLEAN      NOT NULL,
    last_updated    BIGINT       NOT NULL,
    PRIMARY KEY (projection_name, projection_key)
);

CREATE INDEX IF NOT EXISTS akka_projection_name_index ON akka_projection_offset_store (projection_name);

CREATE TABLE IF NOT EXISTS akka_projection_management
(
    projection_name VARCHAR(255) NOT NULL,
    projection_key  VARCHAR(255) NOT NULL,
    paused          BOOLEAN      NOT NULL,
    last_updated    BIGINT       NOT NULL,
    PRIMARY KEY (projection_name, projection_key)
);

CREATE TABLE IF NOT EXISTS akka_projection_timestamp_offset_store
(
    projection_name    VARCHAR(255)             NOT NULL,
    projection_key     VARCHAR(255)             NOT NULL,
    slice              INT                      NOT NULL,
    persistence_id     VARCHAR(255)             NOT NULL,
    seq_nr             BIGINT                   NOT NULL,
    -- timestamp_offset is the db_timestamp of the original event
    timestamp_offset   timestamp with time zone NOT NULL,
    -- timestamp_consumed is when the offset was stored
    -- the consumer lag is timestamp_consumed - timestamp_offset
    timestamp_consumed timestamp with time zone NOT NULL,
    PRIMARY KEY (slice, projection_name, timestamp_offset, persistence_id, seq_nr)
);