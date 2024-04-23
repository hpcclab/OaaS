package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.dispatcher.InvocationReqHolder;
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
  protected boolean shouldLock(InvocationReqHolder reqHolder) {
    return !reqHolder.getReq().immutable();
  }

  @Override
  public void handleRecord(InvocationReqHolder reqHolder) {
    invocationRecordHandler.handleRecord(reqHolder, this::next, false);
  }
}
