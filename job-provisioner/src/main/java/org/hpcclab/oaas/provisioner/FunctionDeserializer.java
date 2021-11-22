package org.hpcclab.oaas.provisioner;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.task.OaasTask;

public class FunctionDeserializer extends ObjectMapperDeserializer<OaasFunctionDto> {
  public FunctionDeserializer() {
    super(OaasFunctionDto.class);
  }
}
