package org.hpcclab.oaas.invoker.service;

import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

import java.util.function.Consumer;

/**
 * @author Pawissanutt
 */
public interface InvocationRecordHandler {
  void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                    InvocationRequest request,
                    Consumer<KafkaConsumerRecord<String, Buffer>> completionHandler,
                    boolean skipDeduplication);

}
