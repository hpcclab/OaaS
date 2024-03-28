package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.service.InvocationRecordHandler;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

@Dependent
public class OrderedInvocationHandlerVerticle extends AbstractOrderedRecordVerticle<InvocationRequest> {
  final InvocationRecordHandler invocationRecordHandler;

  @Inject
  public OrderedInvocationHandlerVerticle(
    InvokerConfig invokerConfig,
    InvocationRecordHandler invocationRecordHandler) {
    super(invokerConfig.invokeConcurrency());
    this.invocationRecordHandler = invocationRecordHandler;
  }

  @Override
  protected boolean shouldLock(KafkaConsumerRecord<String, Buffer> taskRecord,
                               InvocationRequest parsedContent) {
    return !parsedContent.immutable();
  }

  @Override
  protected InvocationRequest parseContent(KafkaConsumerRecord<String, Buffer> taskRecord) {
    return Json.decodeValue(taskRecord.value(), InvocationRequest.class);
  }

  @Override
  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                           InvocationRequest request) {
    invocationRecordHandler.handleRecord(kafkaRecord, request, this::next, false);
  }
}
