package org.hpcclab.msc.taskgen;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.model.Task;

public class ResourceRequestDeserializer extends ObjectMapperDeserializer<ObjectResourceRequest> {
  public ResourceRequestDeserializer() {
    super(ObjectResourceRequest.class);
  }
}
