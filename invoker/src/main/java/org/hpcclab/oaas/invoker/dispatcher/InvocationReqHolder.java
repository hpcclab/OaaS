package org.hpcclab.oaas.invoker.dispatcher;

import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

/**
 * @author Pawissanutt
 */
public interface InvocationReqHolder {
  InvocationRequest getReq();
  String key();
  static KafkaInvocationReqHolder from(KafkaConsumerRecord<String, Buffer> kRecord) {
    return new KafkaInvocationReqHolder(kRecord);
  }
}
