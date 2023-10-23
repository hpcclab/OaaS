package org.hpcclab.oaas.invoker.dispatcher;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.OffsetManager;

public interface RecordDispatcher{
  void setDrainHandler(Runnable drainHandler);
  boolean canConsume();
  void dispatch(KafkaConsumerRecords<?, ?> records);
  Uni<Void> waitTillQueueEmpty();
  OffsetManager getOffsetManager();
}
