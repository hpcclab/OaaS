package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.dataflow.OneShotDataflowInvoker;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Dependent
public class LockingRecordHandlerVerticle extends AbstractVerticle implements RecordHandlerVerticle<KafkaConsumerRecord<String, Buffer>> {
  private static final Logger logger = LoggerFactory.getLogger(LockingRecordHandlerVerticle.class);
  final AtomicInteger inflightCounter = new AtomicInteger(0);
  final InvocationExecutor invocationExecutor;
  final ContextLoader loader;
  final UnifiedFunctionRouter router;
  final OneShotDataflowInvoker dataflowInvoker;
  final ConcurrentLinkedQueue<KafkaConsumerRecord<String, Buffer>> taskQueue;
  private final int maxConcurrent;
  Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler;
  String name = "unknown";

  @Inject
  public LockingRecordHandlerVerticle(InvokerConfig invokerConfig,
                                      InvocationExecutor invocationExecutor,
                                      ContextLoader loader,
                                      UnifiedFunctionRouter router,
                                      OneShotDataflowInvoker dataflowInvoker) {
    this.invocationExecutor = invocationExecutor;
    this.loader = loader;
    this.router = router;
    this.dataflowInvoker = dataflowInvoker;
    this.taskQueue = new ConcurrentLinkedQueue<>();
    this.maxConcurrent = invokerConfig.invokeConcurrency();
  }

  @Override
  public void setOnRecordCompleteHandler(Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler) {
    this.onRecordCompleteHandler = onRecordCompleteHandler;
  }

  @Override
  public void offer(KafkaConsumerRecord<String, Buffer> taskRecord) {
    taskQueue.offer(taskRecord);
    if (inflightCounter.get() < maxConcurrent) {

    }
  }

  private void consume() {

  }


  InvocationRequest parseContent(KafkaConsumerRecord<String, Buffer> taskRecord) {
    return Json.decodeValue(taskRecord.value(), InvocationRequest.class);
  }

  @Override
  public int countQueueingTasks() {
    return inflightCounter.get();
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }
}
