package org.hpcclab.oaas.controller.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;

@ApplicationScoped
public class RpcProducer {

  @Produces
  ProtoMapper protoMapper() {
    return new ProtoMapperImpl();
  }
}
