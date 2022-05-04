CREATE TABLE public.translation_upload (
                                   version bigint not null,
                                   uploaded timestamp without time zone not null,
                                   user_id varchar(120) not null
);

ALTER TABLE ONLY public.translation_upload
    ADD CONSTRAINT translation_upload_pkey PRIMARY KEY (version);

CREATE SEQUENCE public.translation_upload_version
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.translation_upload_version OWNED BY public.translation_upload.version;

CREATE TABLE public.dictionary (
                         english_phrase varchar(64000) not null,
                         translation_phrase varchar(64000),
                         translation_version bigint
);

ALTER TABLE ONLY public.dictionary
    ADD CONSTRAINT fk_dictionary_translation_upload FOREIGN KEY (translation_version)
        REFERENCES public.translation_upload(version)