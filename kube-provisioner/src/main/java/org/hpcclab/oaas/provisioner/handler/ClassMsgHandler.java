package org.hpcclab.oaas.provisioner.handler;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.provisioner.provisioner.KafkaProvisioner;
import org.hpcclab.oaas.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClassMsgHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassMsgHandler.class);

  @Inject
  KafkaProvisioner kafkaProvisioner;
  @Inject
  ClassRepository classRepository;

  @Incoming("clsProvisions")
  @RunOnVirtualThread
  public void handle(Record<String, OaasClass> clsRecord) {
    if (clsRecord.value()==null)
      return;
    var cls = clsRecord.value();
    if (cls.isMarkForRemoval())
      return;
    var updater = kafkaProvisioner.provision(clsRecord);
    if (updater!=null) {
      classRepository.compute(cls.getKey(), (k, v) -> {
        updater.accept(v);
        return v;
      });
    }
  }
}
