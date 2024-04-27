package org.hpcclab.oaas.invoker.dispatcher;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invoker.mq.OffsetManager;

import java.util.List;
import java.util.function.Consumer;

public interface RecordDispatcher{
  void setOnQueueDrained(Runnable drainHandler);
  boolean canConsume();
  void dispatch(List<InvocationReqHolder> invocationReqHolder);
  Uni<Void> waitTillQueueEmpty();
  void setOnRecordDone(Consumer<InvocationReqHolder> onRecordDone);
  void setOnRecordReceived(Consumer<InvocationReqHolder> onRecordReceived);
}
