-- Table: public.scan_logs

-- DROP TABLE IF EXISTS public.scan_logs;

CREATE TABLE IF NOT EXISTS public.scan_logs
(
    id integer NOT NULL DEFAULT nextval('scan_logs_id_seq'::regclass),
    scan_id integer,
    log_level character varying(50) COLLATE pg_catalog."default",
    message character varying(500) COLLATE pg_catalog."default",
    "timestamp" character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT scan_logs_pkey PRIMARY KEY (id),
    CONSTRAINT fk_scanlogs_scan FOREIGN KEY (scan_id)
        REFERENCES public.scans (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)


ALTER TABLE IF EXISTS public.scan_logs

GRANT ALL ON TABLE public.scan_logs TO cyber;

-- Index: idx_scan_logs_scan_id

-- DROP INDEX IF EXISTS public.idx_scan_logs_scan_id;

CREATE INDEX IF NOT EXISTS idx_scan_logs_scan_id
    ON public.scan_logs USING btree
    (scan_id ASC NULLS LAST)
-- Index: idx_scan_logs_timestamp

-- DROP INDEX IF EXISTS public.idx_scan_logs_timestamp;

CREATE INDEX IF NOT EXISTS idx_scan_logs_timestamp
    ON public.scan_logs USING btree
    ("timestamp" COLLATE pg_catalog."default" DESC NULLS FIRST)
