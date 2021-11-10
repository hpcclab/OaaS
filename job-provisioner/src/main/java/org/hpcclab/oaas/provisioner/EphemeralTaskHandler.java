package org.hpcclab.oaas.provisioner;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;
import org.hpcclab.oaas.model.task.OaasTask;
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
  public void handle(@Context CloudEvent<OaasTask> cloudEvent) {
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


}
