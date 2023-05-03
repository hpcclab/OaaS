package org.hpcclab.oaas.invoker.verticle;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.OffLoader;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Dependent
@Deprecated(forRemoval = true)
public class OrderedTaskInvokerVerticle extends AbstractOrderedRecordVerticle<OaasTask> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderedTaskInvokerVerticle.class);
  final OffLoader invoker;
  final FunctionRepository funcRepo;
  final InvocationExecutor graphExecutor;
  final ObjectCompletionPublisher objCompPublisher;
  private final Timer invokingTimer;
  private final Timer completionTimer;

  @Inject
  public OrderedTaskInvokerVerticle(OffLoader invoker,
                                    FunctionRepository funcRepo,
                                    InvocationExecutor graphExecutor,
                                    ObjectCompletionPublisher objCompPublisher,
                                    InvokerConfig invokerConfig,
                                    MeterRegistry registry) {
    super(invokerConfig.invokeConcurrency());
    this.invoker = invoker;
    this.funcRepo = funcRepo;
    this.graphExecutor = graphExecutor;
    this.objCompPublisher = objCompPublisher;
    if (registry!=null) {
      this.invokingTimer = Timer.builder("invoker." + name + ".invoking")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);
      this.completionTimer = Timer.builder("invoker." + name + ".completion")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);
    } else {
      this.invokingTimer = null;
      this.completionTimer = null;
    }
  }

  @Override
  protected boolean shouldLock(KafkaConsumerRecord<String, Buffer> taskRecord, OaasTask parsedContent) {
    return !parsedContent.isImmutable();
  }

  @Override
  protected OaasTask parseContent(KafkaConsumerRecord<String, Buffer> taskRecord) {
    return Json.decodeValue(taskRecord.value(), OaasTask.class);
  }

  @Override
  public void handleRecord(KafkaConsumerRecord<String, Buffer> taskRecord, OaasTask task) {
    var startTime = System.currentTimeMillis();
    var id = task.getId();
    if (LOGGER.isDebugEnabled()) {
      logLatency(task);
    }
    generateInvokingDetail(taskRecord, task)
      .flatMap(invokingDetail -> {
        var invokedUni = invoker.offload(invokingDetail)
          .onFailure()
          .recoverWithItem(err -> TaskCompletion.error(
              TaskIdentity.decode(invokingDetail.getId()),
              err.getMessage(),
              startTime,
              System.currentTimeMillis()
            )
          );

        if (LOGGER.isDebugEnabled()) {
          invokedUni = invokedUni.invoke(() -> LOGGER.debug(
            "{}: task[{}]: invoked in {} ms",
            name, id, System.currentTimeMillis() - startTime));
        }

        return invokedUni
          .map(com -> Tuples.pair(task, com));
      })
//      .flatMap(pair -> handleComplete(pair))
      .flatMap(this::handleComplete)
      .onFailure()
      .recoverWithItem(this::handleFailInvocation)
      .subscribe()
      .with(
        item -> {
          if (item!=null) {
            var taskId = item.getTwo().getId();
            objCompPublisher.publish(taskId.oId()==null ? taskId.mId():taskId.oId());
          }
          next(taskRecord);
        },
        error -> {
          LOGGER.error("Unexpected error on invoker ", error);
          next(taskRecord);
        }
      );
  }

  Uni<Pair<OaasTask, TaskCompletion>> handleComplete(Pair<OaasTask, TaskCompletion> pair) {
    var task = pair.getOne();
    var completion = pair.getTwo();
    try {
      var invokingTime = completion.getCptTs() - completion.getSmtTs();
      if (invokingTimer!=null)
        invokingTimer.record(invokingTime, TimeUnit.MILLISECONDS);
      var ts = System.currentTimeMillis();
      var uni = graphExecutor.complete(task, completion)
        .onFailure()
        .transform(e -> new InvocationException(e, completion))

        .replaceWith(pair);
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("{} persist task[{}] in {} ms",
          name,
          task.getId(),
          System.currentTimeMillis() - ts
        );
      if (completionTimer!=null)
        uni = uni.eventually(() ->
          completionTimer.record(System.currentTimeMillis() - ts, TimeUnit.MILLISECONDS));
      return uni;
    } catch (DecodeException decodeException) {
      return Uni.createFrom().failure(new InvocationException(decodeException, completion));
    }
  }

  Pair<OaasTask, TaskCompletion> handleFailInvocation(Throwable exception) {
    if (exception instanceof InvocationException invocationException) {
      var msg = invocationException.getCause()!=null ? invocationException
        .getCause().getMessage():null;
      if (LOGGER.isWarnEnabled())
        LOGGER.warn("Catch invocation fail on '{}' with message '{}'",
          invocationException.getTaskCompletion().getId().encode(),
          msg,
          invocationException
        );
      // TODO send to dead letter topic
    }
    return null;
  }

  Uni<InvokingDetail<Buffer>> generateInvokingDetail(
    KafkaConsumerRecord<String, Buffer> rec,
    OaasTask task) {
    return funcRepo.getAsync(task.getFuncKey())
      .flatMap(function -> {
        var cond = function.getDeploymentStatus().getCondition();
        if (cond==DeploymentCondition.DELETED) {
          return Uni.createFrom()
            .failure(new StdOaasException("Function was deleted"));
        } else if (cond==DeploymentCondition.RUNNING) {
          return Uni.createFrom()
            .item(function);
        } else {
          return funcRepo.getBypassCacheAsync(task.getFuncKey())
            .onItem().ifNull()
            .failWith(() -> new StdOaasException("Function is not ready"))
            .onFailure(StdOaasException.class)
            .retry().withBackOff(Duration.ofMillis(500))
            .expireIn(5000);
        }
      })
      .invoke(task::setFunction)
      .map(function -> new InvokingDetail<Buffer>(
        task.getId(),
        task.getFuncKey(),
        function.getDeploymentStatus().getInvocationUrl(),
        rec.value(),
        System.currentTimeMillis()
      ));
  }

  void logLatency(OaasTask task) {
    var submittedTs = task.getTs();
    LOGGER.debug("{}: task[{}]: Kafka latency {} ms",
      name,
      task.getId(),
      System.currentTimeMillis() - submittedTs
    );
  }
}
