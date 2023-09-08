package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.OffLoader;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.dataflow.OneShotDataflowInvoker;
import org.hpcclab.oaas.invoker.InvocationRecordHandler;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class OrderedInvocationHandlerVerticle extends AbstractOrderedRecordVerticle<InvocationRequest> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderedInvocationHandlerVerticle.class);
  final InvocationRecordHandler invocationRecordHandler;

  @Inject
  public OrderedInvocationHandlerVerticle(
    InvokerConfig invokerConfig,
    InvocationRecordHandler invocationRecordHandler) {
    super(invokerConfig.invokeConcurrency());
    this.invocationRecordHandler = invocationRecordHandler;
    invocationRecordHandler.setCompletionHandler(this::next);
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
  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
    invocationRecordHandler.handleRecord(kafkaRecord, request);
  }
}
