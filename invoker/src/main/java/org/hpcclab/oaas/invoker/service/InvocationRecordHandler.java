package org.hpcclab.oaas.invoker.service;

import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.invoker.dispatcher.InvocationReqHolder;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

import java.util.function.Consumer;

/**
 * @author Pawissanutt
 */
public interface InvocationRecordHandler {
  void handleRecord(InvocationReqHolder reqHolder,
                    Consumer<InvocationReqHolder> completionHandler,
                    boolean skipDeduplication);

}
