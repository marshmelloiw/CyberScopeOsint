-- Table: public.scan_targets

-- DROP TABLE IF EXISTS public.scan_targets;

CREATE TABLE IF NOT EXISTS public.scan_targets
(
    id integer NOT NULL DEFAULT nextval('scan_targets_id_seq'::regclass),
    scan_id integer,
    target character varying(50) COLLATE pg_catalog."default",
    target_type character varying(50) COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    created_at character varying(50) COLLATE pg_catalog."default",
    processed_at character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT scan_targets_pkey PRIMARY KEY (id),
    CONSTRAINT fk_scantargets_scan FOREIGN KEY (scan_id)
        REFERENCES public.scans (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)


ALTER TABLE IF EXISTS public.scan_targets

GRANT ALL ON TABLE public.scan_targets TO cyber;

-- Index: idx_scan_targets_scan_id

-- DROP INDEX IF EXISTS public.idx_scan_targets_scan_id;

CREATE INDEX IF NOT EXISTS idx_scan_targets_scan_id
    ON public.scan_targets USING btree
    (scan_id ASC NULLS LAST)
-- Index: idx_scan_targets_target

-- DROP INDEX IF EXISTS public.idx_scan_targets_target;

CREATE INDEX IF NOT EXISTS idx_scan_targets_target
    ON public.scan_targets USING btree
    (target COLLATE pg_catalog."default" ASC NULLS LAST)
