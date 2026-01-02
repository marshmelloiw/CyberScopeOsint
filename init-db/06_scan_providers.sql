-- Table: public.scan_providers

-- DROP TABLE IF EXISTS public.scan_providers;

CREATE TABLE IF NOT EXISTS public.scan_providers
(
    id integer NOT NULL DEFAULT nextval('scan_providers_id_seq'::regclass),
    scan_id integer,
    provider_name character varying(50) COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    error_message character varying(500) COLLATE pg_catalog."default",
    created_at character varying(50) COLLATE pg_catalog."default",
    completed_at character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT scan_providers_pkey PRIMARY KEY (id),
    CONSTRAINT fk_scanproviders_scan FOREIGN KEY (scan_id)
        REFERENCES public.scans (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

GRANT ALL ON TABLE public.scan_providers TO cyber;

-- Index: idx_scan_providers_provider_name

-- DROP INDEX IF EXISTS public.idx_scan_providers_provider_name;

CREATE INDEX IF NOT EXISTS idx_scan_providers_provider_name
    ON public.scan_providers USING btree
    (provider_name COLLATE pg_catalog."default" ASC NULLS LAST);

-- Index: idx_scan_providers_scan_id

-- DROP INDEX IF EXISTS public.idx_scan_providers_scan_id;

CREATE INDEX IF NOT EXISTS idx_scan_providers_scan_id
    ON public.scan_providers USING btree
    (scan_id ASC NULLS LAST);
