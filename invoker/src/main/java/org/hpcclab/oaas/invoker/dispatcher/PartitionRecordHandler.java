package org.hpcclab.oaas.invoker.dispatcher;

import java.util.function.Consumer;

public interface PartitionRecordHandler<T> {
    void offer(T rec);
    int countPending();
    void setOnRecordCompleteHandler(Consumer<T> onRecordCompleteHandler);
}
