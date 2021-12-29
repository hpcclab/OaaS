package org.hpcclab.oaas.taskmanager.deserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.ObjectResourceRequest;

public class ResourceRequestDeserializer extends ObjectMapperDeserializer<ObjectResourceRequest> {
  public ResourceRequestDeserializer() {
    super(ObjectResourceRequest.class);
  }
}
