-- Table: public.users

-- DROP TABLE IF EXISTS public.users;

CREATE TABLE IF NOT EXISTS public.users
(
    user_id integer NOT NULL DEFAULT nextval('users_user_id_seq'::regclass),
    email character varying(50) COLLATE pg_catalog."default",
    password_hash character varying(64) COLLATE pg_catalog."default",
    phone_number character varying(50) COLLATE pg_catalog."default",
    sms_mfa_enabled character varying(50) COLLATE pg_catalog."default",
    totp_secret character varying(50) COLLATE pg_catalog."default",
    full_name character varying(50) COLLATE pg_catalog."default",
    role character varying(50) COLLATE pg_catalog."default",
    is_verified character varying(50) COLLATE pg_catalog."default",
    mfa_enabled character varying(50) COLLATE pg_catalog."default",
    created_at character varying(50) COLLATE pg_catalog."default",
    last_login character varying(50) COLLATE pg_catalog."default",
    user_file character varying(128) COLLATE pg_catalog."default",
    CONSTRAINT users_pkey PRIMARY KEY (user_id)
)


ALTER TABLE IF EXISTS public.users

GRANT ALL ON TABLE public.users TO cyber;

-- Index: idx_users_created_at

-- DROP INDEX IF EXISTS public.idx_users_created_at;

CREATE INDEX IF NOT EXISTS idx_users_created_at
    ON public.users USING btree
    (created_at COLLATE pg_catalog."default" DESC NULLS FIRST)
-- Index: idx_users_is_verified

-- DROP INDEX IF EXISTS public.idx_users_is_verified;

CREATE INDEX IF NOT EXISTS idx_users_is_verified
    ON public.users USING btree
    (is_verified COLLATE pg_catalog."default" ASC NULLS LAST)
-- Index: idx_users_phone_number

-- DROP INDEX IF EXISTS public.idx_users_phone_number;

CREATE INDEX IF NOT EXISTS idx_users_phone_number
    ON public.users USING btree
    (phone_number COLLATE pg_catalog."default" ASC NULLS LAST)
-- Index: idx_users_role

-- DROP INDEX IF EXISTS public.idx_users_role;

CREATE INDEX IF NOT EXISTS idx_users_role
    ON public.users USING btree
    (role COLLATE pg_catalog."default" ASC NULLS LAST)
-- Index: idx_users_user_file

-- DROP INDEX IF EXISTS public.idx_users_user_file;

CREATE INDEX IF NOT EXISTS idx_users_user_file
    ON public.users USING btree
    (user_file COLLATE pg_catalog."default" ASC NULLS LAST)
