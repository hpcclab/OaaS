package org.hpcclab.oaas.invoker.dispatcher;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

/**
 * @author Pawissanutt
 */
public class KafkaInvocationReqHolder implements InvocationReqHolder {

  final KafkaConsumerRecord<String, Buffer> kafkaRecord;
  InvocationRequest request;

  public KafkaInvocationReqHolder(KafkaConsumerRecord<String, Buffer> kafkaRecord) {
    this.kafkaRecord = kafkaRecord;
  }

  public KafkaConsumerRecord<String, Buffer> getKafkaRecord() {
    return kafkaRecord;
  }

  @Override
  public InvocationRequest getReq() {
    if (request == null)
      request = Json.decodeValue(kafkaRecord.value(), InvocationRequest.class);
    return request;
  }

  @Override
  public String key() {
    return kafkaRecord.key();
  }

  @Override
  public String toString() {
    return "{"+key() + ", " + kafkaRecord.offset() + "}";
  }
}
