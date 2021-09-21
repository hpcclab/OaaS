
SET 'auto.offset.reset' = 'earliest';
SET 'cache.max.bytes.buffering' = '0';

DROP TABLE AG_T;
DROP TABLE AGG_T;
DROP TABLE TEST_T;
CREATE TABLE TEST_T
(
  k STRING PRIMARY KEY,
  req int,
  ref string
)
WITH (
  KAFKA_TOPIC = 'TEST_T',
  PARTITIONS = 1,
  FORMAT = 'JSON'
);

DROP TABLE COM_T;
CREATE TABLE COM_T
(
  k String PRIMARY KEY,
  v string
)
WITH (
  KAFKA_TOPIC = 'COM_T',
  FORMAT = 'JSON',
  PARTITIONS = 1
);

CREATE TABLE AGG_T AS
SELECT
  t.k AS KEY,
  t.req AS REQ,
  COUNT(*) as c
FROM TEST_T t INNER JOIN COM_T c ON t.ref = c.k
GROUP BY t.k, t.req;

CREATE TABLE AG_T AS
SELECT
  t.k AS KEY,
  t.req AS REQ,
  t.ref AS ref
FROM TEST_T t INNER JOIN COM_T c ON t.ref = c.k;

INSERT INTO TEST_T(k, req,ref) VALUES ('next', 2, 'req1');
INSERT INTO TEST_T(k, req,ref) VALUES ('next', 2, 'req2');
INSERT INTO COM_T(k, v) VALUES ('req1', 'test');
INSERT INTO COM_T(k, v) VALUES ('req2', 'test');
SELECT * FROM AG_T;
SELECT * FROM AGG_T;
