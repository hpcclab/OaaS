package org.hpcclab.oaas.invoker.verticle;

import java.util.function.Consumer;

public interface RecordHandlerVerticle<T> {
  void setOnRecordCompleteHandler(Consumer<T> onRecordCompleteHandler);
  void offer(T taskRecord);
  int countQueueingTasks();
}
