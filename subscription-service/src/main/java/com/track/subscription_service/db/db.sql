

-- Table: subscription

-- DROP TABLE IF EXISTS public.subscription;

CREATE TABLE IF NOT EXISTS public.subscription
(
    id bigint NOT NULL DEFAULT nextval('subscription_id_seq'::regclass),
    name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    type character varying(255) COLLATE pg_catalog."default" NOT NULL,
    duration character varying(255) COLLATE pg_catalog."default",
    cost double precision NOT NULL,
    user_id bigint,
    category character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT subscription_pkey PRIMARY KEY (id),
    CONSTRAINT fk_user FOREIGN KEY (user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT subscription_type_check CHECK (type::text = ANY (ARRAY['recurring'::character varying::text, 'one-time'::character varying::text]))
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.subscription
    OWNER to postgres;

-- Table: public.users

-- DROP TABLE IF EXISTS public.users;

CREATE TABLE IF NOT EXISTS public.users
(
    id bigint NOT NULL DEFAULT nextval('users_id_seq'::regclass),
    google_id character varying(255) COLLATE pg_catalog."default" NOT NULL,
    email character varying(255) COLLATE pg_catalog."default" NOT NULL,
    name character varying(255) COLLATE pg_catalog."default",
    provider character varying(255) COLLATE pg_catalog."default" DEFAULT 'Google'::character varying,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_google_id_key UNIQUE (google_id)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.users
    OWNER to postgres;

-- Table: public.user_preferences

-- DROP TABLE IF EXISTS public.user_preferences;

CREATE TABLE user_preferences (
    user_id BIGINT PRIMARY KEY,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    theme VARCHAR(10) NOT NULL DEFAULT 'light',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);