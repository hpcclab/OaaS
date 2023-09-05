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
import java.util.function.Consumer;

@Dependent
public class InvocationRecordHandler {

  final OffLoader invoker;
  final InvocationExecutor invocationExecutor;
  final ContextLoader loader;
  final UnifiedFunctionRouter router;
  final OneShotDataflowInvoker dataflowInvoker;

  Consumer<KafkaConsumerRecord<String, Buffer>> completionHandler;

  @Inject
  public InvocationRecordHandler(OffLoader invoker, InvocationExecutor invocationExecutor, ContextLoader loader, UnifiedFunctionRouter router, OneShotDataflowInvoker dataflowInvoker) {
    this.invoker = invoker;
    this.invocationExecutor = invocationExecutor;
    this.loader = loader;
    this.router = router;
    this.dataflowInvoker = dataflowInvoker;
  }

  private static final Logger logger = LoggerFactory.getLogger( InvocationRecordHandler.class );
  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
    if (logger.isDebugEnabled()) {
      logLatency(kafkaRecord, request);
    }
    if (request.macro()) {
      handleMacro(kafkaRecord, request);
    } else {
      invokeTask(kafkaRecord, request);
    }
  }
  private void handleMacro(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
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
        ctx -> completionHandler.accept(kafkaRecord),
        error -> {
          logger.error("Unexpected error on invoker ", error);
          completionHandler.accept(kafkaRecord);
        }
      );
  }

  private void invokeTask(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
    if (logger.isDebugEnabled())
      logger.debug("invokeTask {}", request);
    loader.loadCtxAsync(request)
      .flatMap(ctx -> {
        if (detectDuplication(kafkaRecord, ctx)) {
          logger.warn("detect duplication [main={}, out={}]",
            ctx.getRequest().main(), ctx.getRequest().outId());
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
        ctx -> completionHandler.accept(kafkaRecord),
        error -> {
          logger.error("Get an unrecoverable repeating error on invoker ", error);
          completionHandler.accept(kafkaRecord);
        });
  }

  private boolean detectDuplication(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                                    InvocationContext ctx) {
    if (ctx.isImmutable()) {
      return false;
    }
    var obj = ctx.getMain();
    logger.debug("checking duplication [{},{},{}]",
      kafkaRecord.offset(), ctx.getRequest(), obj);
    return obj.getStatus().getUpdatedOffset() >= kafkaRecord.offset();
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

  public void setCompletionHandler(Consumer<KafkaConsumerRecord<String, Buffer>> completionHandler) {
    this.completionHandler = completionHandler;
  }
}
