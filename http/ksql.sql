insert into resourcerequests (ownerObjectId, requestFile) values ('test', 'test.ts');

select * from detected_resourcerequests;
print DISTINCT_RESOURCEREQUESTS from beginning;
select * from DISTINCT_RESOURCEREQUESTS emit changes;


CREATE STREAM test
(
  k string key,
  test VARCHAR,
  test2 VARCHAR
)
WITH (kafka_topic='test', FORMAT='json', PARTITIONS=1);
INSERT into test (k, test, test2) values ('te', 'aa', 'bb');

CREATE table test_table
(
  k string PRIMARY key,
  test VARCHAR,
  test2 VARCHAR
)
WITH (kafka_topic='test', FORMAT='json');
select * from test_table;
