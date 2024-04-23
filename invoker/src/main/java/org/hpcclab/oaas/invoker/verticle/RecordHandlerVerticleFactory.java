package org.hpcclab.oaas.invoker.verticle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.cls.OClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class RecordHandlerVerticleFactory implements VerticleFactory<RecordHandlerVerticle> {
  private static final Logger logger = LoggerFactory.getLogger(RecordHandlerVerticleFactory.class);
  final Instance<OrderedInvocationHandlerVerticle> orderedInvokerVerticleInstance;
  final Instance<LockingRecordHandlerVerticle> lockingInvokerVerticleInstance;
  final InvokerConfig config;

  public RecordHandlerVerticleFactory(Instance<OrderedInvocationHandlerVerticle> orderedInvokerVerticleInstance, Instance<LockingRecordHandlerVerticle> lockingInvokerVerticleInstance, InvokerConfig config) {
    this.orderedInvokerVerticleInstance = orderedInvokerVerticleInstance;
    this.lockingInvokerVerticleInstance = lockingInvokerVerticleInstance;
    this.config = config;
  }

  @Override
  public List<RecordHandlerVerticle> createVerticles(OClass cls) {
    Supplier<RecordHandlerVerticle> supplier;
    if (config.clusterLock()) {
      logger.warn("The experimental 'Cluster lock' is enabled. LockingRecordHandlerVerticle will be used.");
      supplier = lockingInvokerVerticleInstance::get;
    } else {
      supplier = orderedInvokerVerticleInstance::get;
    }
    return IntStream.range(0, config.numOfInvokerVerticle())
      .mapToObj(i -> {
        RecordHandlerVerticle recordHandlerVerticle = supplier.get();
        recordHandlerVerticle.setName("record-handler-" + cls.getKey() + "-" + i);
        return recordHandlerVerticle;
      })
      .toList();
  }
}
