insert into translation_upload values
  (1, '2022-05-06 11:12:13.000000', 'IdamUser1'),
  (2, '2022-05-07 09:00:05.000000', 'IdamUser2'),
  (3, '2022-05-08 16:25:01.000000', 'IdamUser1');

insert into dictionary values
  (1, 'English Phrase 1', null, 1),
  (2, 'English Phrase 2', 'Translated Phrase 2', 2);

 insert into dictionary values
 (3, 'English Phrase 3', 'Translated Phrase 1', 3, true, 'Yes Translation', 'No Translation');

ALTER SEQUENCE translation_version_seq RESTART WITH 4;
ALTER SEQUENCE dictionary_id_seq RESTART WITH 4;
