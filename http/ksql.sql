create stream resourceRequests (ownerObjectId VARCHAR, requestFile VARCHAR)
WITH (kafka_topic='msc-resource-requests', value_format='json');

create table resourceRequestView AS select * from resourceRequests;


select * from tasks emit changes;
