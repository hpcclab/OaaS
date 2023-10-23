package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.invoker.dispatcher.PartitionRecordHandler;

import java.util.function.Consumer;

public interface RecordHandlerVerticle<T> extends Verticle, PartitionRecordHandler<T> {
  void setName(String name);

  String getName();
}
