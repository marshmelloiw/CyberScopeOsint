-- ============================================
-- V4: Optimize Database Structure
-- Daha sade ve anlaşılır veritabanı yapısı
-- ============================================

-- NOT: scan_id -> uuid değişikliği yapılmıyor çünkü kod tarafında scanId kullanılıyor
-- Sadece gereksiz kolonlar ve tablolar temizleniyor

-- ============================================
-- 1. SCANS tablosunu optimize et
-- ============================================
-- scan_id'yi tutuyoruz (kod ile uyumlu)
-- Gereksiz kolonları kontrol et, priority ve schedule_id şimdilik tutuyoruz

-- ============================================
-- 2. SCAN_TARGETS basitleştir
-- ============================================
-- status ve processed_at gereksiz (scan durumu yeterli)
-- target_type -> entity_type (daha açıklayıcı)

DO $$ 
BEGIN
    -- Kolonları kontrol et ve kaldır
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_targets' AND column_name = 'status') THEN
        ALTER TABLE scan_targets DROP COLUMN status;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_targets' AND column_name = 'processed_at') THEN
        ALTER TABLE scan_targets DROP COLUMN processed_at;
    END IF;
    
    -- Kolon adını değiştir
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_targets' AND column_name = 'target_type') THEN
        ALTER TABLE scan_targets RENAME COLUMN target_type TO entity_type;
    END IF;
END $$;

-- ============================================
-- 3. SCAN_PROVIDERS basitleştir
-- ============================================
-- status, error_message, completed_at gereksiz (scan durumu yeterli)

DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_providers' AND column_name = 'status') THEN
        ALTER TABLE scan_providers DROP COLUMN status;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_providers' AND column_name = 'error_message') THEN
        ALTER TABLE scan_providers DROP COLUMN error_message;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_providers' AND column_name = 'completed_at') THEN
        ALTER TABLE scan_providers DROP COLUMN completed_at;
    END IF;
    
    -- provider_name -> provider (daha kısa)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_providers' AND column_name = 'provider_name') THEN
        ALTER TABLE scan_providers RENAME COLUMN provider_name TO provider;
    END IF;
END $$;

-- ============================================
-- 4. SCAN_RESULTS optimize et
-- ============================================
-- provider_name -> provider (tutarlılık için)

DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_results' AND column_name = 'provider_name') THEN
        ALTER TABLE scan_results RENAME COLUMN provider_name TO provider;
    END IF;
END $$;

-- ============================================
-- 5. SCAN_LOGS basitleştir
-- ============================================
-- log_level -> level (daha kısa)
-- timestamp -> created_at (tutarlılık için)

DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_logs' AND column_name = 'log_level') THEN
        ALTER TABLE scan_logs RENAME COLUMN log_level TO level;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'scan_logs' AND column_name = 'timestamp') THEN
        ALTER TABLE scan_logs RENAME COLUMN timestamp TO created_at;
    END IF;
END $$;

-- ============================================
-- 6. ENTITIES optimize et
-- ============================================
-- last_scan_id gereksiz (JOIN ile bulunabilir), kaldır

DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'entities' AND column_name = 'last_scan_id') THEN
        ALTER TABLE entities DROP COLUMN last_scan_id;
    END IF;
END $$;

-- ============================================
-- 7. Gereksiz tabloları kaldır (boş olanlar)
-- ============================================
-- Bu tablolar henüz kullanılmıyor, kaldırılabilir
-- DİKKAT: Eğer veri varsa önce yedek alın!

DROP TABLE IF EXISTS report_scans CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS user_api_keys CASCADE;
DROP TABLE IF EXISTS integration_api_keys CASCADE;
DROP TABLE IF EXISTS scheduled_scans CASCADE;
DROP TABLE IF EXISTS entity_findings CASCADE;
DROP TABLE IF EXISTS api_keys CASCADE;  -- Eski tablo

-- ============================================
-- 8. İndeksleri güncelle
-- ============================================

-- Eski indeksleri kaldır
DROP INDEX IF EXISTS idx_scan_targets_type;
DROP INDEX IF EXISTS idx_scan_providers_name;

-- Yeni indeksler (kolon değişikliklerinden sonra)
CREATE INDEX IF NOT EXISTS idx_scan_targets_entity_type ON scan_targets(entity_type);
CREATE INDEX IF NOT EXISTS idx_scan_providers_provider ON scan_providers(provider);
CREATE INDEX IF NOT EXISTS idx_scan_results_provider ON scan_results(provider);

-- ============================================
-- 9. Yorum ekle (documentation)
-- ============================================

COMMENT ON TABLE scans IS 'OSINT tarama kayıtları - Ana tablo';
COMMENT ON COLUMN scans.scan_id IS 'Benzersiz tarama UUID (API için kullanılır)';
COMMENT ON COLUMN scans.id IS 'Primary key (internal)';

COMMENT ON TABLE scan_targets IS 'Tarama hedefleri - Bir tarama birden fazla hedef içerebilir';
COMMENT ON TABLE scan_providers IS 'Kullanılan OSINT provider''ları (Shodan, VirusTotal, vb.)';
COMMENT ON TABLE scan_results IS 'Tarama sonuçları - Provider''lardan gelen JSON veriler';
COMMENT ON TABLE scan_logs IS 'Tarama log mesajları';
COMMENT ON TABLE entities IS 'Taranan entity''ler (domain, IP, email) - Tekil kayıtlar';

