CREATE TABLE tracked_object(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE geofence(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    polygon JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE position_event(
    id BIGSERIAL PRIMARY KEY,
    object_id UUID NOT NULL REFERENCES tracked_object(id),
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_position_object_time ON position_event (object_id, recorded_at DESC);

CREATE TABLE geofence_alert(
    id BIGSERIAL PRIMARY KEY,
    object_id UUID NOT NULL REFERENCES tracked_object(id),
    geofence_id UUID NOT NULL REFERENCES geofence(id),
    event_type TEXT NOT NULL CHECK (event_type IN ('entered', 'exited')),
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_alert_object_time ON geofence_alert (object_id, triggered_at DESC);
