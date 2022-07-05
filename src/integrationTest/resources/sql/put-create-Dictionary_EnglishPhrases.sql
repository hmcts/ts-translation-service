insert into translation_upload values
  (1, '2022-05-06 11:12:13.000000', 'IdamUser1');

insert into dictionary values
  (1, 'english_1', 'translated_1', 1);
insert into dictionary values
  (2, 'english_2', 'translated_2', 1);

ALTER SEQUENCE translation_version_seq RESTART WITH 2;
ALTER SEQUENCE dictionary_id_seq RESTART WITH 3;

