package org.hpcclab.oaas.pm.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.FunctionRepository;

@GrpcService
public class FunctionServiceImpl implements FunctionService {
  final FunctionRepository fnRepo;
  final ProtoMapper mapper;

  @Inject
  public FunctionServiceImpl(FunctionRepository fnRepo, ProtoMapper mapper) {
    this.fnRepo = fnRepo;
    this.mapper = mapper;
  }

  @Override
  public Uni<ProtoOFunction> get(SingleKeyQuery request) {
    return fnRepo.async().getAsync(request.getKey())
      .map(mapper::toProto);
  }

  @Override
  public Multi<ProtoOFunction> list(PaginateQuery request) {
    return fnRepo.getQueryService().paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(page -> Multi.createFrom().iterable(page.getItems()))
      .map(mapper::toProto);
  }

  @Override
  public Multi<ProtoOFunction> select(MultiKeyQuery request) {
    return fnRepo.async()
      .listAsync(request.getKeyList())
      .toMulti()
      .flatMap(l -> Multi.createFrom().iterable(l.values()))
      .map(mapper::toProto);
  }
}
