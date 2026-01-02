-- Table: public.scans

-- DROP TABLE IF EXISTS public.scans;

CREATE TABLE IF NOT EXISTS public.scans
(
    id integer NOT NULL DEFAULT nextval('scans_id_seq'::regclass),
    scan_id character varying(50) COLLATE pg_catalog."default",
    name character varying(50) COLLATE pg_catalog."default",
    type character varying(50) COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    user_id integer,
    created_at character varying(50) COLLATE pg_catalog."default",
    started_at character varying(50) COLLATE pg_catalog."default",
    completed_at character varying(50) COLLATE pg_catalog."default",
    error_message character varying(50) COLLATE pg_catalog."default",
    priority character varying(50) COLLATE pg_catalog."default",
    schedule_id character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT scans_pkey PRIMARY KEY (id),
    CONSTRAINT fk_scans_user FOREIGN KEY (user_id)
        REFERENCES public.users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL
);

GRANT ALL ON TABLE public.scans TO cyber;

-- Index: idx_scans_created_at

-- DROP INDEX IF EXISTS public.idx_scans_created_at;

CREATE INDEX IF NOT EXISTS idx_scans_created_at
    ON public.scans USING btree
    (created_at COLLATE pg_catalog."default" DESC NULLS FIRST);

-- Index: idx_scans_scan_id

-- DROP INDEX IF EXISTS public.idx_scans_scan_id;

CREATE INDEX IF NOT EXISTS idx_scans_scan_id
    ON public.scans USING btree
    (scan_id COLLATE pg_catalog."default" ASC NULLS LAST);

-- Index: idx_scans_status

-- DROP INDEX IF EXISTS public.idx_scans_status;

CREATE INDEX IF NOT EXISTS idx_scans_status
    ON public.scans USING btree
    (status COLLATE pg_catalog."default" ASC NULLS LAST);

-- Index: idx_scans_type

-- DROP INDEX IF EXISTS public.idx_scans_type;

CREATE INDEX IF NOT EXISTS idx_scans_type
    ON public.scans USING btree
    (type COLLATE pg_catalog."default" ASC NULLS LAST);

-- Index: idx_scans_user_id

-- DROP INDEX IF EXISTS public.idx_scans_user_id;

CREATE INDEX IF NOT EXISTS idx_scans_user_id
    ON public.scans USING btree
    (user_id ASC NULLS LAST);
