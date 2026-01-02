-- Table: public.notifications

-- DROP TABLE IF EXISTS public.notifications;

CREATE TABLE IF NOT EXISTS public.notifications
(
    id integer NOT NULL DEFAULT nextval('notifications_id_seq'::regclass),
    user_id bigint,
    scan_id bigint,
    risk_score numeric(3,1) NOT NULL,
    risk_level character varying(20) COLLATE pg_catalog."default" NOT NULL,
    message text COLLATE pg_catalog."default" NOT NULL,
    is_read boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_scan_id_fkey FOREIGN KEY (scan_id)
        REFERENCES public.scans (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES public.users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

GRANT ALL ON TABLE public.notifications TO cyber;

