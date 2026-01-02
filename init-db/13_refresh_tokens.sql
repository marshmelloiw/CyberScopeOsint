-- Table: public.refresh_tokens

-- DROP TABLE IF EXISTS public.refresh_tokens;

CREATE TABLE IF NOT EXISTS public.refresh_tokens
(
    id bigint NOT NULL DEFAULT nextval('refresh_tokens_id_seq'::regclass),
    user_id bigint NOT NULL,
    token character varying(255) COLLATE pg_catalog."default" NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_token_key UNIQUE (token),
    CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES public.users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

GRANT ALL ON TABLE public.refresh_tokens TO cyber;

-- Index: idx_refresh_tokens_user_id

-- DROP INDEX IF EXISTS public.idx_refresh_tokens_user_id;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON public.refresh_tokens USING btree
    (user_id ASC NULLS LAST);
