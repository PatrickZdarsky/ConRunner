ALTER TABLE eventcatalog_event
    ADD COLUMN source_mode VARCHAR(16) NOT NULL DEFAULT 'MANAGED',
    ADD COLUMN operation_state VARCHAR(24) NOT NULL DEFAULT 'PLANNING',
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    ADD CONSTRAINT ck_schedule_event_mode CHECK (
        (source_mode = 'MANAGED' AND operation_state IN ('PLANNING', 'LIVE'))
        OR (source_mode = 'SYNCED' AND operation_state IN ('AWAITING_IMPORT', 'LIVE'))
    );

ALTER TABLE eventcatalog_location
    ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'LOCAL' CHECK (origin IN ('LOCAL', 'IMPORTED')),
    ADD COLUMN retired BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sort_position INTEGER NOT NULL DEFAULT 0 CHECK (sort_position >= 0),
    ADD CONSTRAINT uq_schedule_location_event_id UNIQUE (event_id, id);

ALTER TABLE eventcatalog_session
    ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'LOCAL' CHECK (origin IN ('LOCAL', 'IMPORTED')),
    ADD COLUMN effective_status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (effective_status IN ('SCHEDULED', 'CANCELLED', 'WITHDRAWN')),
    ADD COLUMN effective_content JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(effective_content) = 'object'),
    ADD COLUMN source_removal_snapshot JSONB CHECK (jsonb_typeof(source_removal_snapshot) = 'object'),
    ADD CONSTRAINT uq_schedule_session_event_id UNIQUE (event_id, id),
    ADD CONSTRAINT fk_schedule_session_event_location FOREIGN KEY (event_id, location_id)
        REFERENCES eventcatalog_location (event_id, id);

CREATE TABLE schedule_signals (
    event_id UUID PRIMARY KEY REFERENCES eventcatalog_event(id) ON DELETE CASCADE,
    collaboration_revision BIGINT NOT NULL DEFAULT 0 CHECK (collaboration_revision >= 0),
    sync_revision BIGINT NOT NULL DEFAULT 0 CHECK (sync_revision >= 0)
);

CREATE TABLE schedule_session_baseline (
    occurrence_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    presence VARCHAR(16) NOT NULL CHECK (presence IN ('PRESENT', 'REMOVED')),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL CHECK (end_time > start_time),
    location_id UUID,
    content JSONB NOT NULL CHECK (jsonb_typeof(content) = 'object'),
    revision BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (event_id, occurrence_id) REFERENCES eventcatalog_session (event_id, id) ON DELETE CASCADE,
    FOREIGN KEY (event_id, location_id) REFERENCES eventcatalog_location (event_id, id)
);

CREATE INDEX ix_schedule_baseline_event ON schedule_session_baseline (event_id);

CREATE TABLE schedule_change_batch (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES eventcatalog_event(id),
    state VARCHAR(16) NOT NULL CHECK (state IN ('DRAFT', 'COMMITTED', 'DISCARDED')),
    origin VARCHAR(24) NOT NULL CHECK (origin IN ('MANUAL', 'PLANNING', 'SYNC', 'CONSOLIDATION', 'STATE_CHANGE')),
    sequence BIGINT,
    created_by UUID,
    last_edited_by UUID,
    committed_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    committed_at TIMESTAMP WITH TIME ZONE,
    idempotency_key VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (event_id, id),
    UNIQUE (event_id, sequence),
    UNIQUE (event_id, idempotency_key),
    CHECK (
        (state = 'COMMITTED' AND sequence IS NOT NULL AND sequence > 0 AND committed_at IS NOT NULL)
        OR (state <> 'COMMITTED' AND sequence IS NULL AND committed_at IS NULL
            AND committed_by IS NULL)
    )
);

CREATE UNIQUE INDEX ux_schedule_current_draft ON schedule_change_batch (event_id) WHERE state = 'DRAFT';

CREATE TABLE schedule_change_entry (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    batch_id UUID NOT NULL,
    occurrence_id UUID,
    reserved_occurrence_id UUID,
    location_id UUID,
    state VARCHAR(16) NOT NULL CHECK (state IN ('DRAFT', 'COMMITTED', 'DISCARDED')),
    operation_type VARCHAR(32) NOT NULL,
    entry_order INTEGER NOT NULL CHECK (entry_order >= 0),
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    schema_version INTEGER NOT NULL CHECK (schema_version > 0),
    reason TEXT,
    created_by UUID,
    last_edited_by UUID,
    UNIQUE (batch_id, entry_order),
    FOREIGN KEY (event_id, batch_id) REFERENCES schedule_change_batch (event_id, id),
    FOREIGN KEY (event_id, occurrence_id) REFERENCES eventcatalog_session (event_id, id),
    FOREIGN KEY (event_id, location_id) REFERENCES eventcatalog_location (event_id, id),
    CHECK (state <> 'COMMITTED' OR reserved_occurrence_id IS NULL OR occurrence_id = reserved_occurrence_id
        AND occurrence_id IS NOT NULL)
);

CREATE INDEX ix_schedule_change_occurrence ON schedule_change_entry (event_id, occurrence_id);
CREATE INDEX ix_schedule_change_location ON schedule_change_entry (event_id, location_id);
