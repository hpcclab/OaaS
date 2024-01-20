package org.hpcclab.oaas.controller.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.controller.model.OrbitMapper;
import org.hpcclab.oaas.controller.model.OrbitMapperImpl;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;

@ApplicationScoped
public class GrpcProducer {

  @Singleton
  @Produces
  ProtoMapper protoMapper() {
    return new ProtoMapperImpl();
  }

  @Singleton
  @Produces
  OrbitMapper orbitMapper() {
    return new OrbitMapperImpl();
  }
}
