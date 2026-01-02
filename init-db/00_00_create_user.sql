-- Create cyber user if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'cyber') THEN
        CREATE USER cyber WITH PASSWORD '123';
        ALTER USER cyber WITH SUPERUSER;
    END IF;
END
$$;

