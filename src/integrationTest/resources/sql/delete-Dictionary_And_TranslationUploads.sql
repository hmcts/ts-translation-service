delete from dictionary;
delete from translation_upload;

ALTER SEQUENCE translation_version_seq RESTART WITH 1;
ALTER SEQUENCE dictionary_id_seq RESTART WITH 1;
