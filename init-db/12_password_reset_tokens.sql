-- Table: public.password_reset_tokens

-- DROP TABLE IF EXISTS public.password_reset_tokens;

CREATE TABLE IF NOT EXISTS public.password_reset_tokens
(
    id integer NOT NULL DEFAULT nextval('password_reset_tokens_id_seq'::regclass),
    email character varying(50) COLLATE pg_catalog."default",
    expires_at character varying(50) COLLATE pg_catalog."default",
    token character varying(50) COLLATE pg_catalog."default",
    user_id integer,
    CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT fk_passwordresettokens_user FOREIGN KEY (user_id)
        REFERENCES public.users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)


ALTER TABLE IF EXISTS public.password_reset_tokens

GRANT ALL ON TABLE public.password_reset_tokens TO cyber;

