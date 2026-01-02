-- Table: public.api_keys

-- DROP TABLE IF EXISTS public.api_keys;

CREATE TABLE IF NOT EXISTS public.api_keys
(
    id integer NOT NULL DEFAULT nextval('api_keys_id_seq'::regclass),
    key_name character varying(50) COLLATE pg_catalog."default",
    api_key character varying(50) COLLATE pg_catalog."default",
    secret_key character varying(50) COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    description character varying(50) COLLATE pg_catalog."default",
    permissions character varying(50) COLLATE pg_catalog."default",
    rate_limit character varying(50) COLLATE pg_catalog."default",
    usage_count integer,
    last_used_at character varying(50) COLLATE pg_catalog."default",
    expires_at character varying(50) COLLATE pg_catalog."default",
    user_id integer,
    created_at character varying(50) COLLATE pg_catalog."default",
    updated_at character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT api_keys_pkey PRIMARY KEY (id),
    CONSTRAINT fk_apikeys_user FOREIGN KEY (user_id)
        REFERENCES public.users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADEa
)


ALTER TABLE IF EXISTS public.api_keys

GRANT ALL ON TABLE public.api_keys TO cyber;

-- Index: idx_api_keys_api_key

-- DROP INDEX IF EXISTS public.idx_api_keys_api_key;

CREATE INDEX IF NOT EXISTS idx_api_keys_api_key
    ON public.api_keys USING btree
    (api_key COLLATE pg_catalog."default" ASC NULLS LAST)
-- Index: idx_api_keys_created_at

-- DROP INDEX IF EXISTS public.idx_api_keys_created_at;

CREATE INDEX IF NOT EXISTS idx_api_keys_created_at
    ON public.api_keys USING btree
    (created_at COLLATE pg_catalog."default" DESC NULLS FIRST)
-- Index: idx_api_keys_status

-- DROP INDEX IF EXISTS public.idx_api_keys_status;

CREATE INDEX IF NOT EXISTS idx_api_keys_status
    ON public.api_keys USING btree
    (status COLLATE pg_catalog."default" ASC NULLS LAST)
-- Index: idx_api_keys_user_id

-- DROP INDEX IF EXISTS public.idx_api_keys_user_id;

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id
    ON public.api_keys USING btree
    (user_id ASC NULLS LAST)
