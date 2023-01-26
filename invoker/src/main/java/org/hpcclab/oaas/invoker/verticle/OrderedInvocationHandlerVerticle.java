package org.hpcclab.oaas.invoker.verticle;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.eclipse.collections.api.tuple.Pair;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.KafkaInvokeException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Dependent
public class OrderedInvocationHandlerVerticle extends AbstractOrderedRecordVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderedInvocationHandlerVerticle.class);
  final SyncInvoker invoker;
  final FunctionRepository funcRepo;
  final InvocationExecutor invocationExecutor;
  final ObjectCompletionPublisher objCompPublisher;
  final ContextLoader loader;
  final UnifiedFunctionRouter router;

  @Inject
  public OrderedInvocationHandlerVerticle(SyncInvoker invoker,
                                          FunctionRepository funcRepo,
                                          InvocationExecutor graphExecutor,
                                          ObjectCompletionPublisher objCompPublisher,
                                          InvokerConfig invokerConfig,
                                          UnifiedFunctionRouter router,
                                          ContextLoader loader) {
    super(invokerConfig.invokeConcurrency());
    this.invoker = invoker;
    this.funcRepo = funcRepo;
    this.invocationExecutor = graphExecutor;
    this.objCompPublisher = objCompPublisher;
    this.router = router;
    this.loader = loader;
  }

  @Override
  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord) {
    var request = Json.decodeValue(kafkaRecord.value(), InvocationRequest.class);
    if (LOGGER.isDebugEnabled()) {
      logLatency(kafkaRecord);
    }
    if (request.macro()) {
      generateMacro(request);
    } else {
      invokeTask(kafkaRecord, request);
    }
  }

  private void generateMacro(InvocationRequest request) {

  }

  private void invokeTask(KafkaConsumerRecord<String, Buffer> kafkaRecord, InvocationRequest request) {
    loader.loadCtxAsync(request)
      .flatMap(ctx -> {
        if (detectDuplication(kafkaRecord, ctx))
          return Uni.createFrom().nullItem();
        return router.apply(ctx)
          .flatMap(invocationExecutor::asyncExec);
      })
      .onFailure()
      .recoverWithItem(this::handleFailInvocation)
      .subscribe()
      .with(ctx -> {
        if (ctx != null && ctx.getOutput() != null)
          objCompPublisher.publish(ctx.getOutput().getId());
        next(kafkaRecord);
      }, error -> {
        LOGGER.error("Unexpected error on invoker ", error);
        next(kafkaRecord);
      });
  }

  private boolean detectDuplication(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                                    FunctionExecContext ctx) {
    var obj = ctx.isImmutable() ? ctx.getOutput():ctx.getMain();
    return obj.getStatus().getUpdatedOffset() >= kafkaRecord.offset();
  }

  FunctionExecContext handleFailInvocation(Throwable exception) {
    if (exception instanceof KafkaInvokeException kafkaInvokeException) {
      var msg = kafkaInvokeException.getCause()!=null ? kafkaInvokeException
        .getCause().getMessage():null;
      if (LOGGER.isWarnEnabled())
        LOGGER.warn("Catch invocation fail on '{}' with message '{}'",
          kafkaInvokeException.getTaskCompletion().getId().encode(),
          msg,
          kafkaInvokeException
        );
      // TODO send to dead letter topic
    }
    return null;
  }

  void logLatency(KafkaConsumerRecord<?, ?> kafkaRecord) {
    var submittedTs = kafkaRecord.timestamp();
    LOGGER.debug("{}: record[{}]: Kafka latency {} ms",
      name,
      kafkaRecord.key(),
      System.currentTimeMillis() - submittedTs
    );
  }
}
