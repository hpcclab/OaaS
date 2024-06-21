package org.hpcclab.oaas.invoker;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.MapEntityRepository;
import org.hpcclab.oaas.test.MockupData;

/**
 * @author Pawissanutt
 */

@GrpcService
class FunctionServiceMock implements FunctionService {
  FunctionRepository fnRepo;
  ProtoMapper protoMapper = new ProtoMapperImpl();

  public FunctionServiceMock() {
    fnRepo = new MapEntityRepository.MapFnRepository(MockupData.testFunctions());
  }

  @Override
  public Uni<ProtoOFunction> get(SingleKeyQuery request) {
    return fnRepo.async().getAsync(request.getKey())
      .map(protoMapper::toProto);
  }

  @Override
  public Multi<ProtoOFunction> list(PaginateQuery request) {
    return Multi.createFrom().empty();
  }

  @Override
  public Multi<ProtoOFunction> select(MultiKeyQuery request) {
    return fnRepo.async().listAsync(request.getKeyList())
      .toMulti().flatMap(m -> Multi.createFrom().iterable(m.values()))
      .map(protoMapper::toProto);
  }
}
