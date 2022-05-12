-- translation_upload table and sequence creation
CREATE SEQUENCE public.translation_version_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.translation_upload (
                                  version bigint not null default nextval('public.translation_version_seq'::regclass),
                                  uploaded timestamp without time zone not null,
                                  user_id varchar(120) not null
);

ALTER TABLE ONLY public.translation_upload
    ADD CONSTRAINT translation_upload_pkey PRIMARY KEY (version);

ALTER SEQUENCE public.translation_version_seq OWNED BY public.translation_upload.version;


-- dictionary table and sequence creation
CREATE SEQUENCE public.dictionary_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.dictionary (
                         id bigint not null default nextval('public.dictionary_id_seq'::regclass),
                         english_phrase varchar(64000) not null,
                         translation_phrase varchar(64000),
                         translation_version bigint
);

ALTER SEQUENCE public.dictionary_id_seq OWNED BY public.dictionary.id;

ALTER TABLE ONLY public.dictionary
    ADD CONSTRAINT dictionary_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dictionary
    ADD CONSTRAINT fk_dictionary_translation_upload FOREIGN KEY (translation_version)
        REFERENCES public.translation_upload(version)
