package org.hpcclab.oaas.provisioner;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;
import org.hpcclab.oaas.model.task.OaasTask;

import javax.inject.Inject;
import javax.ws.rs.core.Context;

public class EphemeralTaskHandler {

  @Inject
  JobProvisioner jobProvisioner;

  @Funq
  @CloudEventMapping(
    trigger = "dev.knative.kafka.event",
    attributes = {
      @EventAttribute(name = "kafkaheadercetype", value = "oaas.task"),
      @EventAttribute(name = "kafkaheadercetasktype", value = "EPHEMERAL")
    }
  )
  public void handle(@Context CloudEvent<OaasTask> cloudEvent) {
    jobProvisioner.provision(cloudEvent.data());
  }



}
