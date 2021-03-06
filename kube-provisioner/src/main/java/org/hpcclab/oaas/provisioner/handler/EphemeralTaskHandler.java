package org.hpcclab.oaas.provisioner.handler;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.provisioner.JobProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Context;

public class EphemeralTaskHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralTaskHandler.class);
  @Inject
  JobProvisioner jobProvisioner;

  @Funq
  @CloudEventMapping(
    trigger = "dev.knative.kafka.event"
  )
  public void handleFromKafka(@Context CloudEvent<OaasTask> cloudEvent) {
    var extMap = cloudEvent.extensions();
    if (!extMap.containsKey("kafkaheadercetype")) {
      LOGGER.warn("Can not handle event without 'kafkaheadercetype' header.");
      return;
    }
    var type = extMap.get("kafkaheadercetype");
    if (!type.equals("oaas.task")) {
      LOGGER.warn("Can not handle event with header 'kafkaheadercetype' is '{}'.",
        type);
      return;
    }
    if (!extMap.containsKey("kafkaheadercetasktype")) {
      LOGGER.warn("Can not handle event without 'kafkaheadercetasktype' header.");
      return;
    }

    var taskType = extMap.get("kafkaheadercetasktype");
    if (!taskType.equals("EPHEMERAL")) {
      LOGGER.warn("Can not handle event with header 'kafkaheadercetasktype' is '{}'.", taskType);
      return;
    }
    jobProvisioner.provision(cloudEvent.data());
  }

  @Funq
  @CloudEventMapping(
    trigger = "oaas.task"
  )
  public void handle(@Context CloudEvent<OaasTask> cloudEvent) {
    var extMap = cloudEvent.extensions();
    var type = cloudEvent.type();
    if (!type.equals("oaas.task")) {
      LOGGER.warn("Can not handle event with type='{}'.",
        type);
      return;
    }
    if (!extMap.containsKey("tasktype")) {
      LOGGER.warn("Can not handle event without 'tasktype' header.");
      return;
    }

    var taskType = extMap.get("tasktype");
    if (!taskType.equals("EPHEMERAL")) {
      LOGGER.warn("Can not handle event with header 'tasktype' is '{}'.", taskType);
      return;
    }
    jobProvisioner.provision(cloudEvent.data());
  }


}
