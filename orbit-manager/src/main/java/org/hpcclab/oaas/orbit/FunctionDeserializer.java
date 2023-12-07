package org.hpcclab.oaas.orbit;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.function.OFunction;

public class FunctionDeserializer extends ObjectMapperDeserializer<OFunction> {
  public FunctionDeserializer() {
    super(OFunction.class);
  }
}
