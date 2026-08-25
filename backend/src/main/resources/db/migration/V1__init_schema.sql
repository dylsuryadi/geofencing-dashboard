CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE tracked_object(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    label TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE geofence(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    geom GEOMETRY(Polygon, 4326) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX geofence_geom_idx ON geofence USING GIST (geom);

CREATE TABLE position_event(
    id BIGSERIAL PRIMARY KEY,
    object_id UUID NOT NULL REFERENCES tracked_object(id),
    location GEOMETRY(Point, 4326) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX position_event_location_idx ON position_event USING GIST (location);

CREATE TABLE geofence_alert(
    id BIGSERIAL PRIMARY KEY,
    object_id UUID NOT NULL REFERENCES tracked_object(id),
    geofence_id UUID NOT NULL REFERENCES geofence(id),
    event_type TEXT NOT NULL CHECK (event_type IN ('entered', 'exited')),
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE object_geofence_state(
    object_id UUID NOT NULL REFERENCES tracked_object(id) ON DELETE CASCADE,
    geofence_id UUID NOT NULL REFERENCES geofence(id) ON DELETE CASCADE,
    is_inside BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (object_id, geofence_id)
);
