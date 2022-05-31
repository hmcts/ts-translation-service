-- Add unique constraint to dictionary.english_phrase

ALTER TABLE ONLY public.dictionary
    ADD CONSTRAINT english_phrase_unique UNIQUE (english_phrase);

--BEGIN;
--  ALTER TABLE ONLY public.dictionary ALTER id DROP DEFAULT; -- drop default
--
--  DROP SEQUENCE public.dictionary_id_seq;              -- drop owned sequence
--
--  ALTER TABLE ONLY public.dictionary
--     ALTER id ADD GENERATED ALWAYS AS IDENTITY (RESTART 1);
--COMMIT;
--
--
--
--
--ALTER TABLE ONLY public.dictionary
--    ALTER COLUMN id TYPE BIGSERIAL GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
--
--DROP SEQUENCE public.dictionary_id_seq;
