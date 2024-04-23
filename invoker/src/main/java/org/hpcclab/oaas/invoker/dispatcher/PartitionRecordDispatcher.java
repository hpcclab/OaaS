package org.hpcclab.oaas.invoker.dispatcher;

import io.netty.util.internal.ThreadLocalRandom;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.mq.OffsetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionRecordDispatcher implements RecordDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(PartitionRecordDispatcher.class);
  private final AtomicInteger inflight = new AtomicInteger(0);
  private final OffsetManager offsetManager;
  private final int maxInflight;
  private final ImmutableList<PartitionRecordHandler> partitions;
  String name = "unknown";
  Random random = ThreadLocalRandom.current();
  private Runnable drainHandler;

  public PartitionRecordDispatcher(OffsetManager offsetManager,
                                   List<? extends PartitionRecordHandler> partitions,
                                   InvokerConfig config) {
    this.offsetManager = offsetManager;
    this.maxInflight = config.maxInflight();
    this.partitions = Lists.immutable.ofAll(partitions);
    for (PartitionRecordHandler partition : partitions) {
      partition.setOnRecordCompleteHandler(this::handleRecordComplete);
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDrainHandler(Runnable drainHandler) {
    this.drainHandler = drainHandler;
  }

  public boolean canConsume() {
    return inflight.get() < maxInflight;
  }

  private void handleRecordComplete(InvocationReqHolder reqHolder) {
    offsetManager.recordDone(reqHolder);
    if (drainHandler!=null && inflight.decrementAndGet() < maxInflight)
      drainHandler.run();
  }

  public void dispatch(List<InvocationReqHolder> records) {
    if (partitions.isEmpty())
      throw new IllegalStateException("Must deploy first");
    for (int i = 0; i < records.size(); i++) {
      var rec = records.get(i);
      offsetManager.recordReceived(rec);
      var partition = partitions.getFirst();
      var size = partitions.size();
      int hashIndex = 0;
      if (size!=1) {
        if (rec.key()!=null) {
          hashIndex = rec.key().hashCode() % size;
          if (hashIndex < 0) hashIndex = -hashIndex;
        } else {
          hashIndex = random.nextInt(size);
        }
        partition = partitions.get(hashIndex);
      }
      if (logger.isDebugEnabled())
        logger.debug("dispatch {} {}", hashIndex, rec);
      partition.offer(rec);
    }
  }

  public Uni<Void> waitTillQueueEmpty() {
    return Multi.createFrom().ticks().every(Duration.ofMillis(100))
      .filter(l -> partitions.stream()
        .mapToInt(PartitionRecordHandler::countPending)
        .sum()==0)
      .toUni()
      .replaceWithVoid();
  }

  public OffsetManager getOffsetManager() {
    return offsetManager;
  }
}
