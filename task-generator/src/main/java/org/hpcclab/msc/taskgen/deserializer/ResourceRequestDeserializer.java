package org.hpcclab.msc.taskgen.deserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.msc.object.model.ObjectResourceRequest;

public class ResourceRequestDeserializer extends ObjectMapperDeserializer<ObjectResourceRequest> {
  public ResourceRequestDeserializer() {
    super(ObjectResourceRequest.class);
  }
}
