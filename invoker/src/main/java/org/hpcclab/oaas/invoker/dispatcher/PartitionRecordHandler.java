package org.hpcclab.oaas.invoker.dispatcher;

import java.util.function.Consumer;

public interface PartitionRecordHandler {
    void offer(InvocationReqHolder rec);
    int countPending();
    void setOnRecordCompleteHandler(Consumer<InvocationReqHolder> onRecordCompleteHandler);
}
