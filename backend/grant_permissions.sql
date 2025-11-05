-- PostgreSQL İzin Verme Scripti
-- Bu script, cyber kullanıcısına tüm tablolar için tam yetki verir

-- Veritabanına bağlan
\c cyberscope

-- Tüm mevcut tablolara SELECT, INSERT, UPDATE, DELETE izinleri ver
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cyber;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO cyber;
GRANT USAGE ON SCHEMA public TO cyber;

-- Gelecekte oluşturulacak tablolar için de otomatik izin verme
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO cyber;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO cyber;

-- Özel tablo izinleri (eğer varsa)
GRANT ALL PRIVILEGES ON TABLE users TO cyber;
GRANT ALL PRIVILEGES ON TABLE entities TO cyber;
GRANT ALL PRIVILEGES ON TABLE entity_findings TO cyber;
GRANT ALL PRIVILEGES ON TABLE scans TO cyber;
GRANT ALL PRIVILEGES ON TABLE scan_targets TO cyber;
GRANT ALL PRIVILEGES ON TABLE scan_providers TO cyber;
GRANT ALL PRIVILEGES ON TABLE scan_results TO cyber;
GRANT ALL PRIVILEGES ON TABLE scan_logs TO cyber;
GRANT ALL PRIVILEGES ON TABLE roles TO cyber;
GRANT ALL PRIVILEGES ON TABLE password_reset_tokens TO cyber;

-- İzinleri kontrol et
SELECT 
    table_schema,
    table_name,
    privilege_type
FROM information_schema.table_privileges
WHERE grantee = 'cyber'
ORDER BY table_name, privilege_type;

