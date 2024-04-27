package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import org.hpcclab.oaas.invoker.dispatcher.PartitionRecordHandler;

public interface RecordHandlerVerticle extends Verticle, PartitionRecordHandler {
  void setName(String name);
  String getName();
}
