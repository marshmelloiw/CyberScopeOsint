-- Create all sequences first
-- This file should run before any table creation

-- API Keys
CREATE SEQUENCE IF NOT EXISTS api_keys_id_seq;

-- Flyway Schema History
CREATE SEQUENCE IF NOT EXISTS flyway_schema_history_installed_rank_seq;

-- Notification Preferences
CREATE SEQUENCE IF NOT EXISTS notification_preferences_id_seq;

-- Notifications
CREATE SEQUENCE IF NOT EXISTS notifications_id_seq;

-- Password Reset Tokens
CREATE SEQUENCE IF NOT EXISTS password_reset_tokens_id_seq;

-- Refresh Tokens
CREATE SEQUENCE IF NOT EXISTS refresh_tokens_id_seq;

-- Roles
CREATE SEQUENCE IF NOT EXISTS roles_id_seq;

-- Scan Logs
CREATE SEQUENCE IF NOT EXISTS scan_logs_id_seq;

-- Scan Providers
CREATE SEQUENCE IF NOT EXISTS scan_providers_id_seq;

-- Scan Results
CREATE SEQUENCE IF NOT EXISTS scan_results_id_seq;

-- Scan Targets
CREATE SEQUENCE IF NOT EXISTS scan_targets_id_seq;

-- Scans
CREATE SEQUENCE IF NOT EXISTS scans_id_seq;

-- Users
CREATE SEQUENCE IF NOT EXISTS users_user_id_seq;

-- User Roles
CREATE SEQUENCE IF NOT EXISTS user_roles_id_seq;
