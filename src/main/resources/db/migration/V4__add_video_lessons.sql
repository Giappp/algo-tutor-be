CREATE TABLE IF NOT EXISTS video_lesson
(
    id                   BIGINT       NOT NULL,
    description          TEXT,
    source_object_key    VARCHAR(500),
    thumbnail_object_key VARCHAR(500),
    duration_seconds     INTEGER,
    file_size_bytes      BIGINT,
    mime_type            VARCHAR(100),
    processing_status    VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_videolesson PRIMARY KEY (id),
    CONSTRAINT fk_videolesson_on_id FOREIGN KEY (id) REFERENCES lessons (id)
);
