package org.hpcclab.oaas.provisioner;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.function.OaasFunction;

public class FunctionDeserializer extends ObjectMapperDeserializer<OaasFunction> {
  public FunctionDeserializer() {
    super(OaasFunction.class);
  }
}
