-- Add unique constraint to dictionary.english_phrase

ALTER TABLE ONLY public.dictionary
    ADD CONSTRAINT english_phrase_unique UNIQUE (english_phrase);
