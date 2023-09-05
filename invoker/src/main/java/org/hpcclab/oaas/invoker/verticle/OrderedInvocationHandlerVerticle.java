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
  //  final OffLoader invoker;
//  final InvocationExecutor invocationExecutor;
//  final ContextLoader loader;
//  final UnifiedFunctionRouter router;
//  final OneShotDataflowInvoker dataflowInvoker;
  final InvocationRecordHandler invocationRecordHandler;

  @Inject
  public OrderedInvocationHandlerVerticle(
    InvokerConfig invokerConfig,
//    OffLoader invoker,
//    InvocationExecutor invocationExecutor,
//    UnifiedFunctionRouter router,
//    ContextLoader loader,
//    OneShotDataflowInvoker dataflowInvoker,
    InvocationRecordHandler invocationRecordHandler) {
    super(invokerConfig.invokeConcurrency());
//    this.invoker = invoker;
//    this.invocationExecutor = graphExecutor;
//    this.router = router;
//    this.loader = loader;
//    this.dataflowInvoker = dataflowInvoker;
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
//    if (LOGGER.isDebugEnabled()) {
//      logLatency(kafkaRecord, request);
//    }
//    if (request.macro()) {
//      handleMacro(kafkaRecord, request);
//    } else {
//      invokeTask(kafkaRecord, request);
//    }
    invocationRecordHandler.handleRecord(kafkaRecord, request);
  }
//
//  private void handleMacro(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
//    loader.loadCtxAsync(request)
//      .flatMap(router::apply)
//      .flatMap(ctx -> {
//        var macro = ctx.getFunction().getMacro();
//        if (macro!=null && macro.isAtomic()) {
//          return dataflowInvoker.invoke(ctx);
//        } else {
//          return invocationExecutor.disaggregateMacro(ctx);
//        }
//      })
//      .onFailure().retry()
//      .withBackOff(Duration.ofMillis(100))
//      .atMost(3)
//      .subscribe()
//      .with(
//        ctx -> next(kafkaRecord),
//        error -> {
//          LOGGER.error("Unexpected error on invoker ", error);
//          next(kafkaRecord);
//        }
//      );
//  }
//
//  private void invokeTask(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
//    if (LOGGER.isDebugEnabled())
//      LOGGER.debug("invokeTask {}", request);
//    loader.loadCtxAsync(request)
//      .flatMap(ctx -> {
//        if (detectDuplication(kafkaRecord, ctx)) {
//          LOGGER.warn("detect duplication [main={}, out={}]",
//            ctx.getRequest().main(), ctx.getRequest().outId());
//          return Uni.createFrom().nullItem();
//        }
//        return router.apply(ctx)
//          .invoke(fec -> fec.setMqOffset(kafkaRecord.offset()))
//          .flatMap(invocationExecutor::asyncExec);
//      })
//      .onFailure(InvocationException.class)
//      .retry().atMost(3)
//      .onFailure()
//      .recoverWithItem(this::handleFailInvocation)
//      .subscribe()
//      .with(
//        ctx -> next(kafkaRecord),
//        error -> {
//          LOGGER.error("Get an unrecoverable repeating error on invoker ", error);
//          next(kafkaRecord);
//        });
//  }

//  private boolean detectDuplication(KafkaConsumerRecord<String, Buffer> kafkaRecord,
//                                    InvocationContext ctx) {
//    if (ctx.isImmutable()) {
//      return false;
//    }
//    var obj = ctx.getMain();
//    LOGGER.debug("checking duplication [{},{},{}]",
//      kafkaRecord.offset(), ctx.getRequest(), obj);
//    return obj.getStatus().getUpdatedOffset() >= kafkaRecord.offset();
//  }
//
//  InvocationContext handleFailInvocation(Throwable exception) {
//    if (exception instanceof InvocationException invocationException) {
//      if (LOGGER.isWarnEnabled())
//        LOGGER.warn("Catch invocation fail on id='{}'",
//          invocationException.getTaskCompletion().getId().encode(),
//          invocationException
//        );
//      // TODO send to dead letter topic
//    } else {
//      LOGGER.error("Unexpected exception", exception);
//    }
//    return null;
//  }
//
//  void logLatency(KafkaConsumerRecord<?, ?> kafkaRecord, InvocationRequest request) {
//    var submittedTs = kafkaRecord.timestamp();
//    LOGGER.debug("{}: record[{},{},{}]: Kafka latency {} ms",
//      name,
//      kafkaRecord.key(),
//      request.invId(),
//      request.macro(),
//      System.currentTimeMillis() - submittedTs
//    );
//  }
}
