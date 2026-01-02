-- Table: public.notification_preferences

-- DROP TABLE IF EXISTS public.notification_preferences;

CREATE TABLE IF NOT EXISTS public.notification_preferences
(
    id integer NOT NULL DEFAULT nextval('notification_preferences_id_seq'::regclass),
    user_id bigint NOT NULL,
    enable_notifications boolean NOT NULL DEFAULT true,
    sound_alerts boolean NOT NULL DEFAULT true,
    category_security boolean NOT NULL DEFAULT true,
    category_scan boolean NOT NULL DEFAULT true,
    category_breach boolean NOT NULL DEFAULT true,
    category_system boolean NOT NULL DEFAULT true,
    category_intelligence boolean NOT NULL DEFAULT true,
    in_app_notifications boolean NOT NULL DEFAULT true,
    email_notifications boolean NOT NULL DEFAULT true,
    push_notifications boolean NOT NULL DEFAULT false,
    digest_frequency character varying(20) COLLATE pg_catalog."default" NOT NULL DEFAULT 'daily'::character varying,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_preferences_pkey PRIMARY KEY (id),
    CONSTRAINT notification_preferences_user_id_key UNIQUE (user_id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id)
        REFERENCES public.users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)


ALTER TABLE IF EXISTS public.notification_preferences

GRANT ALL ON TABLE public.notification_preferences TO cyber;

-- Index: idx_notification_preferences_user_id

-- DROP INDEX IF EXISTS public.idx_notification_preferences_user_id;

CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id
    ON public.notification_preferences USING btree
    (user_id ASC NULLS LAST)
