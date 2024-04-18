package org.hpcclab.oaas.pm.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;

@ApplicationScoped
public class GrpcProducer {

  @Singleton
  @Produces
  ProtoMapper protoMapper() {
    return new ProtoMapperImpl();
  }

}
