package org.hpcclab.oaas.task.handler;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.task.OaasTask;

public class TaskDeserializer extends ObjectMapperDeserializer<OaasTask> {
  public TaskDeserializer() {
    super(OaasTask.class);
  }
}
