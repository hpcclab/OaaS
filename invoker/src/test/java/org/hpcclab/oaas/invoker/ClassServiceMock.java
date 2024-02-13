package org.hpcclab.oaas.invoker;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.test.MapEntityRepository;
import org.hpcclab.oaas.test.MockInvocationEngine;
import org.hpcclab.oaas.test.MockupData;

/**
 * @author Pawissanutt
 */
@GrpcService
class ClassServiceMock implements ClassService {
  ClassRepository clsRepo;
  ProtoMapper protoMapper = new ProtoMapperImpl();

  public ClassServiceMock() {
    clsRepo = new MapEntityRepository.MapClsRepository(MockupData.testClasses());
  }

  @Override
  public Uni<ProtoOClass> get(SingleKeyQuery request) {
    return clsRepo.async().getAsync(request.getKey())
      .map(protoMapper::toProto);
  }

  @Override
  public Multi<ProtoOClass> list(PaginateQuery request) {
    return Multi.createFrom().empty();
  }

  @Override
  public Multi<ProtoOClass> select(MultiKeyQuery request) {
    return clsRepo.async().listAsync(request.getKeyList())
      .toMulti().flatMap(m -> Multi.createFrom().iterable(m.values()))
      .map(protoMapper::toProto);
  }
}
