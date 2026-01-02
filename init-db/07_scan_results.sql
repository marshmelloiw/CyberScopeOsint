-- Table: public.scan_results

-- DROP TABLE IF EXISTS public.scan_results;

CREATE TABLE IF NOT EXISTS public.scan_results
(
    id integer NOT NULL DEFAULT nextval('scan_results_id_seq'::regclass),
    scan_id integer,
    scan_target_id integer,
    provider_name character varying(255) COLLATE pg_catalog."default",
    result_data text COLLATE pg_catalog."default",
    risk_score character varying(100) COLLATE pg_catalog."default",
    risk_level character varying(100) COLLATE pg_catalog."default",
    findings_count integer,
    gemini_report text COLLATE pg_catalog."default",
    created_at character varying(100) COLLATE pg_catalog."default",
    updated_at character varying(100) COLLATE pg_catalog."default",
    CONSTRAINT scan_results_pkey PRIMARY KEY (id),
    CONSTRAINT fk_scanresults_scan FOREIGN KEY (scan_id)
        REFERENCES public.scans (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT fk_scanresults_target FOREIGN KEY (scan_target_id)
        REFERENCES public.scan_targets (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL
)


ALTER TABLE IF EXISTS public.scan_results

GRANT ALL ON TABLE public.scan_results TO cyber;

-- Index: idx_scan_results_provider_name

-- DROP INDEX IF EXISTS public.idx_scan_results_provider_name;

CREATE INDEX IF NOT EXISTS idx_scan_results_provider_name
    ON public.scan_results USING btree
    (provider_name COLLATE pg_catalog."default" ASC NULLS LAST)
-- Index: idx_scan_results_risk_score

-- DROP INDEX IF EXISTS public.idx_scan_results_risk_score;

CREATE INDEX IF NOT EXISTS idx_scan_results_risk_score
    ON public.scan_results USING btree
    (risk_score COLLATE pg_catalog."default" DESC NULLS FIRST)
-- Index: idx_scan_results_scan_id

-- DROP INDEX IF EXISTS public.idx_scan_results_scan_id;

CREATE INDEX IF NOT EXISTS idx_scan_results_scan_id
    ON public.scan_results USING btree
    (scan_id ASC NULLS LAST)
-- Index: idx_scan_results_scan_target_id

-- DROP INDEX IF EXISTS public.idx_scan_results_scan_target_id;

CREATE INDEX IF NOT EXISTS idx_scan_results_scan_target_id
    ON public.scan_results USING btree
    (scan_target_id ASC NULLS LAST)
