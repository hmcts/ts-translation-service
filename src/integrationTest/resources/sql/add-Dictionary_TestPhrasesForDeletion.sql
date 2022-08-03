insert into translation_upload values
  (1, '2022-05-06 11:12:13.000000', 'IdamUser1'),
  (2, '2022-05-07 09:00:05.000000', 'IdamUser2');

insert into dictionary values
  (1, 'TEST-delete-me-no-translation', null, null),
  (2, 'keep-me-no-translation', null, null),
  (3, 'TEST-delete-me-with-translation-1', 'Translated Phrase 1', 1),
  (4, 'keep-me-with-translation-1', 'Translated Phrase 2', 1), -- NB: in same upload as id=3 & 6 which will be deleted
  (5, 'keep-me-with-translation-2', 'Translated Phrase 3', 2),
  (6, 'TEST-delete-me-with-translation-2', 'Translated Phrase 4', 1);

ALTER SEQUENCE translation_version_seq RESTART WITH 3;
ALTER SEQUENCE dictionary_id_seq RESTART WITH 7;
