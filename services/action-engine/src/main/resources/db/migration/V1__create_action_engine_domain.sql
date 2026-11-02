CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    tenant VARCHAR(2) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE site (
    tenant VARCHAR(2) NOT NULL,
    source_id UUID NOT NULL,
    source_version BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    city VARCHAR(100),
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant, source_id)
);

CREATE TABLE contact (
    tenant VARCHAR(2) NOT NULL,
    source_id UUID NOT NULL,
    source_version BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(32),
    preferred_channel VARCHAR(20),
    consent_given BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant, source_id)
);

CREATE TABLE care_subject (
    tenant VARCHAR(2) NOT NULL,
    source_id UUID NOT NULL,
    source_version BIGINT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    owner_source_id VARCHAR(40) NOT NULL,
    species_code VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant, source_id)
);
