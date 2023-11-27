package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import org.hpcclab.oaas.invoker.dispatcher.PartitionRecordHandler;

public interface RecordConsumerVerticle<T> extends Verticle, PartitionRecordHandler<T> {
  void setName(String name);

  String getName();
}
