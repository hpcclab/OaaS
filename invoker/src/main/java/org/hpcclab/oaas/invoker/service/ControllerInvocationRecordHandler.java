package org.hpcclab.oaas.invoker.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.CtxLoader;
import org.hpcclab.oaas.invocation.controller.InvocationCtx;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnObjectRepository;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * @author Pawissanutt
 */
public class ControllerInvocationRecordHandler implements InvocationRecordHandler {
  private static final Logger logger = LoggerFactory.getLogger(ControllerInvocationRecordHandler.class);

  final ObjectRepoManager objectRepoManager;
  final ClassControllerRegistry classControllerRegistry;
  final CtxLoader ctxLoader;

  public ControllerInvocationRecordHandler(ObjectRepoManager objectRepoManager,
                                           ClassControllerRegistry classControllerRegistry,
                                           CtxLoader ctxLoader) {
    this.objectRepoManager = objectRepoManager;
    this.classControllerRegistry = classControllerRegistry;
    this.ctxLoader = ctxLoader;
  }

  @Override
  public void handleRecord(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                           InvocationRequest request, Consumer<KafkaConsumerRecord<String, Buffer>> completionHandler,
                           boolean skipDeduplication) {
    if (logger.isDebugEnabled()) {
      logDebug(kafkaRecord, request);
    }
    ctxLoader.load(request)
      .flatMap(ctx -> {
        if (!skipDeduplication && detectDuplication(kafkaRecord, ctx)) {
          return Uni.createFrom().nullItem();
        }
        var con = classControllerRegistry.getClassController(request.cls());
        return con.invoke(ctx);
      })
      .onFailure(InvocationException.class).retry()
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


  private boolean detectDuplication(KafkaConsumerRecord<String, Buffer> kafkaRecord,
                                    InvocationCtx ctx) {
    var obj = ctx.getMain();
    if (ctx.isImmutable())
      return false;
    if (obj.getLastOffset() < kafkaRecord.offset())
      return false;
    logger.warn("detect duplication [main={}, objOfs={}, reqOfs={}]",
      ctx.getRequest().main(),
      ctx.getMain().getLastOffset(),
      kafkaRecord.offset());
    return true;
  }

  void logDebug(KafkaConsumerRecord<?, ?> kafkaRecord, InvocationRequest request) {
    var submittedTs = kafkaRecord.timestamp();
    var repo = objectRepoManager.getOrCreate(request.cls());
    var cache = ((EIspnObjectRepository) repo).getCache();
    var local = cache.getDistributionManager().getCacheTopology().getSegment(request.main());

    logger.debug("record[{},{}]: Kafka latency {} ms, locality[{}={}]",
      kafkaRecord.key(),
      request.invId(),
      System.currentTimeMillis() - submittedTs,
      kafkaRecord.partition(),
      local
    );
  }


}
