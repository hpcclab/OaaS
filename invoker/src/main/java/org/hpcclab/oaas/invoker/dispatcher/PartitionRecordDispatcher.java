package org.hpcclab.oaas.invoker.dispatcher;

import io.netty.util.internal.ThreadLocalRandom;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PartitionRecordDispatcher implements RecordDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(PartitionRecordDispatcher.class);
  private final AtomicInteger inflight = new AtomicInteger(0);
  private final int maxInflight;
  private final ImmutableList<PartitionRecordHandler> partitions;
  String name = "unknown";
  Random random = ThreadLocalRandom.current();
  private Runnable drainHandler;
  private Consumer<InvocationReqHolder> onRecordReceived;
  private Consumer<InvocationReqHolder> onRecordDone;

  public PartitionRecordDispatcher(List<? extends PartitionRecordHandler> partitions,
                                   InvokerConfig config) {
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

  public void setOnQueueDrained(Runnable drainHandler) {
    this.drainHandler = drainHandler;
  }

  public boolean canConsume() {
    return inflight.get() < maxInflight;
  }

  private void handleRecordComplete(InvocationReqHolder reqHolder) {
    onRecordDone.accept(reqHolder);
    if (drainHandler!=null && inflight.decrementAndGet() < maxInflight)
      drainHandler.run();
  }

  public void dispatch(List<InvocationReqHolder> records) {
    if (partitions.isEmpty())
      throw new IllegalStateException("Must deploy first");
    for (InvocationReqHolder rec : records) {
      onRecordReceived.accept(rec);
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


  @Override
  public void setOnRecordReceived(Consumer<InvocationReqHolder> onRecordReceived) {
    this.onRecordReceived = onRecordReceived;
  }

  @Override
  public void setOnRecordDone(Consumer<InvocationReqHolder> onRecordDone) {
    this.onRecordDone = onRecordDone;
  }
}
