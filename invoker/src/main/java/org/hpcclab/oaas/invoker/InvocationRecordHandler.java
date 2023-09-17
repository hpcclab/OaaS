package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.OffLoader;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.dataflow.OneShotDataflowInvoker;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@Dependent
public class InvocationRecordHandler {

  private static final Logger logger = LoggerFactory.getLogger(InvocationRecordHandler.class);
  final OffLoader invoker;
  final InvocationExecutor invocationExecutor;
  final ContextLoader loader;
  final UnifiedFunctionRouter router;
  final OneShotDataflowInvoker dataflowInvoker;

  @Inject
  public InvocationRecordHandler(OffLoader invoker, InvocationExecutor invocationExecutor, ContextLoader loader, UnifiedFunctionRouter router, OneShotDataflowInvoker dataflowInvoker) {
    this.invoker = invoker;
    this.invocationExecutor = invocationExecutor;
    this.loader = loader;
    this.router = router;
    this.dataflowInvoker = dataflowInvoker;
  }

  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                           InvocationRequest request,
                           BiConsumer<KafkaConsumerRecord<String, Buffer>, InvocationRequest> completionHandler) {
    handleRecord(kafkaRecord, request, completionHandler, null);
  }
  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                           InvocationRequest request,
                           BiConsumer<KafkaConsumerRecord<String, Buffer>, InvocationRequest> completionHandler,
                           BiPredicate<KafkaConsumerRecord<String, Buffer>, InvocationContext> skipCondition) {
    if (logger.isDebugEnabled()) {
      logLatency(kafkaRecord, request);
    }
    if (request.macro()) {
      handleMacro(kafkaRecord, request, completionHandler);
    } else {
      if (skipCondition == null) skipCondition = this::detectDuplication;
      invokeTask(kafkaRecord, request, completionHandler, skipCondition);
    }
  }

  private void handleMacro(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                           InvocationRequest request,
                           BiConsumer<KafkaConsumerRecord<String, Buffer>, InvocationRequest> completionHandler) {
    loader.loadCtxAsync(request)
      .flatMap(router::apply)
      .flatMap(ctx -> {
        var macro = ctx.getFunction().getMacro();
        if (macro!=null && macro.isAtomic()) {
          return dataflowInvoker.invoke(ctx);
        } else {
          return invocationExecutor.disaggregateMacro(ctx);
        }
      })
      .onFailure().retry()
      .withBackOff(Duration.ofMillis(100))
      .atMost(3)
      .subscribe()
      .with(
        ctx -> completionHandler.accept(kafkaRecord, request),
        error -> {
          logger.error("Unexpected error on invoker ", error);
          completionHandler.accept(kafkaRecord, request);
        }
      );
  }

  private void invokeTask(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                          InvocationRequest request,
                          BiConsumer<KafkaConsumerRecord<String, Buffer>, InvocationRequest> completionHandler,
                          BiPredicate<KafkaConsumerRecord<String, Buffer>, InvocationContext> skipCondition) {
    if (logger.isDebugEnabled())
      logger.debug("invokeTask [{},{}] {}", request.main(), kafkaRecord.offset(), request);
    loader.loadCtxAsync(request)
      .flatMap(ctx -> {
        if (skipCondition.test(kafkaRecord, ctx)) {
          return Uni.createFrom().nullItem();
        }
        return router.apply(ctx)
          .invoke(fec -> fec.setMqOffset(kafkaRecord.offset()))
          .flatMap(invocationExecutor::asyncExec);
      })
      .onFailure(InvocationException.class)
      .retry().atMost(3)
      .onFailure()
      .recoverWithItem(this::handleFailInvocation)
      .subscribe()
      .with(
        ctx -> completionHandler.accept(kafkaRecord, request),
        error -> {
          logger.error("Get an unrecoverable repeating error on invoker ", error);
          completionHandler.accept(kafkaRecord, request);
        });
  }

  private boolean detectDuplication(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                                    InvocationContext ctx) {
    var obj = ctx.getMain();
    if (ctx.isImmutable())
      return false;
    if (obj.getStatus().getUpdatedOffset() < kafkaRecord.offset())
      return false;
    logger.warn("detect duplication [main={}, objOfs={}, reqOfs={}]",
      ctx.getRequest().main(),
      ctx.getMain().getStatus().getUpdatedOffset(),
      kafkaRecord.offset());
    return true;
  }

  InvocationContext handleFailInvocation(Throwable exception) {
    if (exception instanceof InvocationException invocationException) {
      if (logger.isWarnEnabled())
        logger.warn("Catch invocation fail on id='{}'",
          invocationException.getTaskCompletion().getId().encode(),
          invocationException
        );
      // TODO send to dead letter topic
    } else {
      logger.error("Unexpected exception", exception);
    }
    return null;
  }

  void logLatency(KafkaConsumerRecord<?, ?> kafkaRecord, InvocationRequest request) {
    var submittedTs = kafkaRecord.timestamp();
    logger.debug("record[{},{},{}]: Kafka latency {} ms",
      kafkaRecord.key(),
      request.invId(),
      request.macro(),
      System.currentTimeMillis() - submittedTs
    );
  }
}
