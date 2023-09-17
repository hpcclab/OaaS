package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;

import java.util.function.Consumer;

public interface RecordHandlerVerticle<T> extends Verticle {
  void setOnRecordCompleteHandler(Consumer<T> onRecordCompleteHandler);
  void offer(T taskRecord);
  int countQueueingTasks();

  void setName(String name);

  String getName();
}
