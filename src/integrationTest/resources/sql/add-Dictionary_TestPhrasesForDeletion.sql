insert into translation_upload values
  (1, '2022-05-06 11:12:13.000000', 'IdamUser1'),
  (2, '2022-05-07 09:00:05.000000', 'IdamUser2');

insert into dictionary values
  (1, 'TEST-delete-me-no-translation', null, null),
  (2, 'keep-me-no-translation', null, null),
  (3, 'TEST-delete-me-with-translation', 'Translated Phrase 1', 1),
  (4, 'keep-me-with-translation', 'Translated Phrase 2', 2);

ALTER SEQUENCE translation_version_seq RESTART WITH 3;
ALTER SEQUENCE dictionary_id_seq RESTART WITH 5;

