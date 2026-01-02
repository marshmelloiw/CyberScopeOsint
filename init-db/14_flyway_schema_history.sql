-- Table: public.flyway_schema_history

-- DROP TABLE IF EXISTS public.flyway_schema_history;

CREATE TABLE IF NOT EXISTS public.flyway_schema_history
(
    installed_rank integer NOT NULL DEFAULT nextval('flyway_schema_history_installed_rank_seq'::regclass),
    version character varying(50) COLLATE pg_catalog."default",
    description character varying(50) COLLATE pg_catalog."default",
    type character varying(50) COLLATE pg_catalog."default",
    script character varying(1000) COLLATE pg_catalog."default",
    checksum integer,
    installed_by character varying(50) COLLATE pg_catalog."default",
    installed_on character varying(50) COLLATE pg_catalog."default",
    execution_time integer,
    success character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT flyway_schema_history_pkey PRIMARY KEY (installed_rank)
)


ALTER TABLE IF EXISTS public.flyway_schema_history

GRANT ALL ON TABLE public.flyway_schema_history TO cyber;

