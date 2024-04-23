package org.hpcclab.oaas.invoker.dispatcher;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.mq.OffsetManager;

import java.util.List;

public interface RecordDispatcher{
  void setDrainHandler(Runnable drainHandler);
  boolean canConsume();
  void dispatch(List<InvocationReqHolder> invocationReqHolder);
  Uni<Void> waitTillQueueEmpty();
  OffsetManager getOffsetManager();
}
