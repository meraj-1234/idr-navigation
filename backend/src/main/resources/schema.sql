-- IDR NAV Database Schema
-- Compatible with PostgreSQL and H2

CREATE TABLE IF NOT EXISTS navigation_sessions (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    initial_latitude DOUBLE PRECISION,
    initial_longitude DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS estimated_positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    velocity DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    source VARCHAR(32) NOT NULL,
    gnss_available BOOLEAN NOT NULL,
    navigation_mode VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS navigation_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    description VARCHAR(512) NOT NULL,
    severity VARCHAR(16) NOT NULL
);
