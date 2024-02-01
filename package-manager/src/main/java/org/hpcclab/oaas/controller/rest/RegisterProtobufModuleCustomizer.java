package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class RegisterProtobufModuleCustomizer implements ObjectMapperCustomizer {

  public void customize(ObjectMapper mapper) {
    mapper.registerModule(new ProtobufModule());
  }
}
