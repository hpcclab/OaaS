package org.hpcclab.oaas.invoker.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.CtxLoader;
import org.hpcclab.oaas.invoker.dispatcher.InvocationReqHolder;
import org.hpcclab.oaas.invoker.dispatcher.KafkaInvocationReqHolder;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnObjectRepository;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * @author Pawissanutt
 */
public class CcInvocationRecordHandler implements InvocationRecordHandler {
  private static final Logger logger = LoggerFactory.getLogger(CcInvocationRecordHandler.class);

  final ObjectRepoManager objectRepoManager;
  final ClassControllerRegistry classControllerRegistry;
  final CtxLoader ctxLoader;

  public CcInvocationRecordHandler(ObjectRepoManager objectRepoManager,
                                   ClassControllerRegistry classControllerRegistry,
                                   CtxLoader ctxLoader) {
    this.objectRepoManager = objectRepoManager;
    this.classControllerRegistry = classControllerRegistry;
    this.ctxLoader = ctxLoader;
  }

  @Override
  public void handleRecord(InvocationReqHolder reqHolder,
                           Consumer<InvocationReqHolder> completionHandler,
                           boolean skipDeduplication) {
    if (logger.isDebugEnabled()) {
      logDebug(reqHolder);
    }
    var req = reqHolder.getReq();
    ctxLoader.load(reqHolder.getReq())
      .flatMap(ctx -> {
        if (!skipDeduplication && detectDuplication(reqHolder, ctx)) {
          return Uni.createFrom().nullItem();
        }
        var con = classControllerRegistry.getClassController(req.cls());
        return con.invoke(ctx);
      })
      .onFailure(InvocationException.class).retry()
      .withBackOff(Duration.ofMillis(100))
      .atMost(3)
      .subscribe()
      .with(
        ctx -> completionHandler.accept(reqHolder),
        error -> {
          logger.error("Unexpected error on invoker ", error);
          completionHandler.accept(reqHolder);
        }
      );
  }


  private boolean detectDuplication(InvocationReqHolder reqHolder,
                                    InvocationCtx ctx) {
    if (reqHolder instanceof KafkaInvocationReqHolder kafkaInvocationReqHolder) {
      KafkaConsumerRecord<String, Buffer> kafkaRecord = kafkaInvocationReqHolder.getKafkaRecord();
      var obj = ctx.getMain();
      if (ctx.isImmutable())
        return false;
      if (obj.getMeta().getLastOffset() < kafkaRecord.offset())
        return false;
      logger.warn("detect duplication [main={}, objOfs={}, reqOfs={}]",
        ctx.getRequest().main(),
        ctx.getMain().getMeta().getLastOffset(),
        kafkaRecord.offset());
      return true;
    }
    return false;
  }

  void logDebug(InvocationReqHolder reqHolder) {
    if (reqHolder instanceof KafkaInvocationReqHolder kafkaInvocationReqHolder) {
      KafkaConsumerRecord<String, Buffer> kafkaRecord = kafkaInvocationReqHolder.getKafkaRecord();
      var request = reqHolder.getReq();
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


}
