-- Add YesOrNo Columns to dictionary table

ALTER TABLE ONLY public.dictionary
  ADD COLUMN "yes_or_no" boolean DEFAULT FALSE,
  ADD COLUMN "yes" varchar(120),
  ADD COLUMN "no" varchar(120);
