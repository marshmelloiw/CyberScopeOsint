-- CyberScope OSINT Database Schema
-- V2: Create main OSINT tables

-- ============================================
-- SCANS TABLE
-- Tarama işlemlerini saklar
-- ============================================
CREATE TABLE IF NOT EXISTS scans (
    id BIGSERIAL PRIMARY KEY,
    scan_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    schedule_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_scans_user_id ON scans(user_id);
CREATE INDEX IF NOT EXISTS idx_scans_status ON scans(status);
CREATE INDEX IF NOT EXISTS idx_scans_type ON scans(type);
CREATE INDEX IF NOT EXISTS idx_scans_created_at ON scans(created_at);
CREATE INDEX IF NOT EXISTS idx_scans_scan_id ON scans(scan_id);

-- ============================================
-- SCAN_TARGETS TABLE
-- Tarama hedeflerini saklar (birden fazla target olabilir)
-- ============================================
CREATE TABLE IF NOT EXISTS scan_targets (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    target VARCHAR(500) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scan_targets_scan_id ON scan_targets(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_targets_target ON scan_targets(target);
CREATE INDEX IF NOT EXISTS idx_scan_targets_type ON scan_targets(target_type);

-- ============================================
-- SCAN_PROVIDERS TABLE
-- Tarama için kullanılan provider'ları saklar
-- ============================================
CREATE TABLE IF NOT EXISTS scan_providers (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    provider_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scan_providers_scan_id ON scan_providers(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_providers_name ON scan_providers(provider_name);

-- ============================================
-- SCAN_RESULTS TABLE
-- Tarama sonuçlarını JSON olarak saklar
-- ============================================
CREATE TABLE IF NOT EXISTS scan_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    scan_target_id BIGINT REFERENCES scan_targets(id) ON DELETE CASCADE,
    provider_name VARCHAR(100) NOT NULL,
    
    -- Sonuç verileri JSON olarak saklanır
    result_data JSONB NOT NULL,
    
    -- Risk skoru ve özet bilgileri
    risk_score DECIMAL(3,1),  -- 0.0 - 10.0
    risk_level VARCHAR(20),  -- LOW, MEDIUM, HIGH, CRITICAL
    findings_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scan_results_scan_id ON scan_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_results_target_id ON scan_results(scan_target_id);
CREATE INDEX IF NOT EXISTS idx_scan_results_provider ON scan_results(provider_name);
CREATE INDEX IF NOT EXISTS idx_scan_results_risk_score ON scan_results(risk_score);
CREATE INDEX IF NOT EXISTS idx_scan_results_risk_level ON scan_results(risk_level);
CREATE INDEX IF NOT EXISTS idx_scan_results_data_gin ON scan_results USING GIN (result_data);

-- ============================================
-- SCAN_LOGS TABLE
-- Tarama loglarını saklar
-- ============================================
CREATE TABLE IF NOT EXISTS scan_logs (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    log_level VARCHAR(20) DEFAULT 'INFO',
    message TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scan_logs_scan_id ON scan_logs(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_logs_timestamp ON scan_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_scan_logs_level ON scan_logs(log_level);

-- ============================================
-- ENTITIES TABLE
-- Taranan entity'leri (domain, IP, email) saklar
-- ============================================
CREATE TABLE IF NOT EXISTS entities (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_value VARCHAR(500) NOT NULL,
    last_scan_id BIGINT REFERENCES scans(id) ON DELETE SET NULL,
    last_scanned_at TIMESTAMP,
    risk_score DECIMAL(3,1),
    risk_level VARCHAR(20),
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_value)
);

CREATE INDEX IF NOT EXISTS idx_entities_type ON entities(entity_type);
CREATE INDEX IF NOT EXISTS idx_entities_value ON entities(entity_value);
CREATE INDEX IF NOT EXISTS idx_entities_risk_score ON entities(risk_score);
CREATE INDEX IF NOT EXISTS idx_entities_last_scanned ON entities(last_scanned_at);

-- ============================================
-- ENTITY_FINDINGS TABLE
-- Entity'lere ait bulguları saklar (CVE, breach, etc.)
-- ============================================
CREATE TABLE IF NOT EXISTS entity_findings (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    scan_id BIGINT REFERENCES scans(id) ON DELETE SET NULL,
    finding_type VARCHAR(50) NOT NULL,  -- 'CVE', 'BREACH', 'VULNERABILITY', 'MALWARE', 'SUSPICIOUS'
    finding_id VARCHAR(255),  -- CVE ID, breach name, etc.
    title VARCHAR(500),
    description TEXT,
    severity VARCHAR(20),  -- LOW, MEDIUM, HIGH, CRITICAL
    cvss_score DECIMAL(3,1),
    source VARCHAR(100),  -- Which provider found this
    raw_data JSONB,  -- Original finding data
    
    discovered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE'
);

CREATE INDEX IF NOT EXISTS idx_entity_findings_entity_id ON entity_findings(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_findings_scan_id ON entity_findings(scan_id);
CREATE INDEX IF NOT EXISTS idx_entity_findings_type ON entity_findings(finding_type);
CREATE INDEX IF NOT EXISTS idx_entity_findings_severity ON entity_findings(severity);
CREATE INDEX IF NOT EXISTS idx_entity_findings_status ON entity_findings(status);

-- ============================================
-- REPORTS TABLE
-- Oluşturulan raporları saklar
-- ============================================
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- 'scan', 'monthly', 'custom', 'dashboard'
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    
    -- Rapor içeriği
    content JSONB,  -- Rapor detayları JSON olarak
    summary TEXT,
    
    -- İstatistikler
    total_scans INTEGER DEFAULT 0,
    total_findings INTEGER DEFAULT 0,
    average_risk_score DECIMAL(3,1),
    
    -- Durum ve metadata
    status VARCHAR(50) DEFAULT 'DRAFT',  -- DRAFT, GENERATING, COMPLETED, FAILED
    format VARCHAR(20) DEFAULT 'HTML',  -- HTML, PDF, JSON
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reports_user_id ON reports(user_id);
CREATE INDEX IF NOT EXISTS idx_reports_type ON reports(type);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(created_at);

-- ============================================
-- REPORT_SCANS TABLE
-- Raporlarda kullanılan scan'leri ilişkilendirir
-- ============================================
CREATE TABLE IF NOT EXISTS report_scans (
    report_id BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    scan_id BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    PRIMARY KEY (report_id, scan_id)
);

CREATE INDEX IF NOT EXISTS idx_report_scans_report_id ON report_scans(report_id);
CREATE INDEX IF NOT EXISTS idx_report_scans_scan_id ON report_scans(scan_id);

-- ============================================
-- NOTIFICATIONS TABLE
-- Bildirimleri saklar
-- ============================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    
    -- Bildirim içeriği
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,  -- 'info', 'warning', 'error', 'success', 'alert'
    severity VARCHAR(20),  -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- İlişkili kayıtlar
    related_scan_id BIGINT REFERENCES scans(id) ON DELETE SET NULL,
    related_entity_id BIGINT REFERENCES entities(id) ON DELETE SET NULL,
    related_finding_id BIGINT REFERENCES entity_findings(id) ON DELETE SET NULL,
    
    -- Link ve metadata
    action_url VARCHAR(500),
    read BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_scan_id ON notifications(related_scan_id);

-- ============================================
-- USER_API_KEYS TABLE
-- Kullanıcı bazlı API anahtarları
-- ============================================
CREATE TABLE IF NOT EXISTS user_api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Key bilgileri
    name VARCHAR(255) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,  -- Hashed API key
    key_prefix VARCHAR(20),  -- İlk karakterler (gösterim için)
    
    -- İzinler
    scopes TEXT[],  -- Array of scopes: ['read', 'write', 'admin']
    
    -- Durum ve kullanım
    status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, REVOKED, EXPIRED
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_api_keys_user_id ON user_api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_key_hash ON user_api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_status ON user_api_keys(status);

-- ============================================
-- INTEGRATION_API_KEYS TABLE
-- Shodan, VirusTotal, HIBP gibi entegrasyonlar için API key'ler
-- ============================================
CREATE TABLE IF NOT EXISTS integration_api_keys (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,  -- 'Shodan', 'VirusTotal', 'HIBP'
    api_key_encrypted TEXT NOT NULL,  -- Encrypted API key
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,  -- NULL = global/system key
    
    -- Metadata
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_validated_at TIMESTAMP,
    usage_count BIGINT DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_integration_keys_provider ON integration_api_keys(provider);
CREATE INDEX IF NOT EXISTS idx_integration_keys_user_id ON integration_api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_integration_keys_status ON integration_api_keys(status);

-- ============================================
-- AUDIT_LOGS TABLE
-- Sistem denetim kayıtları
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    
    -- Action bilgileri
    action VARCHAR(100) NOT NULL,  -- 'login', 'scan_create', 'report_generate', etc.
    resource_type VARCHAR(100),  -- 'Scan', 'User', 'Report', etc.
    resource_id BIGINT,
    
    -- Detaylar
    description TEXT,
    ip_address VARCHAR(45),  -- IPv4 or IPv6
    user_agent TEXT,
    request_data JSONB,
    
    -- Sonuç
    status VARCHAR(20) DEFAULT 'SUCCESS',  -- SUCCESS, FAILED
    error_message TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- ============================================
-- SCHEDULED_SCANS TABLE (Gelecek için)
-- Zamanlanmış taramalar
-- ============================================
CREATE TABLE IF NOT EXISTS scheduled_scans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Schedule bilgileri
    schedule_type VARCHAR(50) NOT NULL,  -- 'once', 'daily', 'weekly', 'monthly', 'cron'
    schedule_config JSONB,  -- Cron expression veya schedule detayları
    
    -- Scan konfigürasyonu
    scan_type VARCHAR(50) NOT NULL,
    targets TEXT[] NOT NULL,
    providers TEXT[] NOT NULL,
    
    -- Durum
    status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, PAUSED, DISABLED
    next_run_at TIMESTAMP,
    last_run_at TIMESTAMP,
    last_run_scan_id BIGINT REFERENCES scans(id) ON DELETE SET NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scheduled_scans_user_id ON scheduled_scans(user_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_scans_status ON scheduled_scans(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_scans_next_run ON scheduled_scans(next_run_at);

-- ============================================
-- Update triggers for updated_at columns
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_scan_results_updated_at BEFORE UPDATE ON scan_results
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_integration_api_keys_updated_at BEFORE UPDATE ON integration_api_keys
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_scheduled_scans_updated_at BEFORE UPDATE ON scheduled_scans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

