package org.hpcclab.oaas.crm.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;

@ApplicationScoped
public class GrpcProducer {

  @Produces
  @Singleton
  public ProtoMapper protoMapper() {
    return new ProtoMapperImpl();
  }
}
