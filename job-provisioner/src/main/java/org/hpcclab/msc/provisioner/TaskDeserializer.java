package org.hpcclab.msc.provisioner;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.entity.task.OaasTask;

public class TaskDeserializer extends ObjectMapperDeserializer<OaasTask> {
  public TaskDeserializer() {
    super(OaasTask.class);
  }
}
