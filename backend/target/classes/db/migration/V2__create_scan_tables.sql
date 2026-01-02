-- CyberScope OSINT Database Schema
-- V2: Create scan tables for storing scan history and results

-- ============================================
-- SCANS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS scans (
    id BIGSERIAL PRIMARY KEY,
    scan_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    priority VARCHAR(20),
    schedule_id BIGINT,
    CONSTRAINT fk_scans_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_scans_scan_id ON scans(scan_id);
CREATE INDEX IF NOT EXISTS idx_scans_user_id ON scans(user_id);
CREATE INDEX IF NOT EXISTS idx_scans_status ON scans(status);
CREATE INDEX IF NOT EXISTS idx_scans_type ON scans(type);
CREATE INDEX IF NOT EXISTS idx_scans_created_at ON scans(created_at DESC);

-- ============================================
-- SCAN_TARGETS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS scan_targets (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    target VARCHAR(500) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT fk_scan_targets_scan FOREIGN KEY (scan_id) REFERENCES scans(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scan_targets_scan_id ON scan_targets(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_targets_target ON scan_targets(target);

-- ============================================
-- SCAN_PROVIDERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS scan_providers (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_scan_providers_scan FOREIGN KEY (scan_id) REFERENCES scans(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scan_providers_scan_id ON scan_providers(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_providers_provider_name ON scan_providers(provider_name);

-- ============================================
-- SCAN_RESULTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS scan_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    scan_target_id BIGINT,
    provider_name VARCHAR(100) NOT NULL,
    result_data JSONB NOT NULL,
    risk_score DECIMAL(3,1),
    risk_level VARCHAR(20),
    findings_count INTEGER DEFAULT 0,
    gemini_report JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scan_results_scan FOREIGN KEY (scan_id) REFERENCES scans(id) ON DELETE CASCADE,
    CONSTRAINT fk_scan_results_target FOREIGN KEY (scan_target_id) REFERENCES scan_targets(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_scan_results_scan_id ON scan_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_results_scan_target_id ON scan_results(scan_target_id);
CREATE INDEX IF NOT EXISTS idx_scan_results_provider_name ON scan_results(provider_name);
CREATE INDEX IF NOT EXISTS idx_scan_results_risk_score ON scan_results(risk_score DESC);

-- ============================================
-- SCAN_LOGS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS scan_logs (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    log_level VARCHAR(20) DEFAULT 'INFO',
    message TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scan_logs_scan FOREIGN KEY (scan_id) REFERENCES scans(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scan_logs_scan_id ON scan_logs(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_logs_timestamp ON scan_logs(timestamp DESC);

