-- CyberScope OSINT Database Schema
-- V1: Initial schema matching existing database structure

-- ============================================
-- USERS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: user_id, email, password_hash, full_name, role, is_verified, mfa_enabled, created_at, last_login

-- ============================================
-- ENTITIES TABLE  
-- ============================================
-- Note: This table already exists in the database
-- Structure: entity_id, user_id, entity_type, entity_value, created_at, last_scan_at

-- ============================================
-- API_KEYS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: key_id, service_name, api_key, owner_id, created_at, expires_at, status

-- ============================================
-- LOGS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: log_id, user_id, action, ip_address, timestamp, result

-- ============================================
-- NOTIFICATIONS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: notification_id, user_id, message, channel, sent_at, is_read

-- ============================================
-- REPORTS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: report_id, user_id, report_type, report_data, file_path, created_at

-- ============================================
-- THREAT_EVENTS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: event_id, entity_id, source, risk_score, details, status, detected_at

-- ============================================
-- RECOMMENDATIONS TABLE
-- ============================================
-- Note: This table already exists in the database
-- Structure: recommendation_id, event_id, recommendation_text, created_at

-- This migration file is a placeholder.
-- The actual database schema already exists and matches the Java entity models.
-- Flyway will baseline this migration on first run.

