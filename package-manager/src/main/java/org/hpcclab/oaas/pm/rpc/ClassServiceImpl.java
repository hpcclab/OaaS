package org.hpcclab.oaas.pm.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.ClassRepository;

@GrpcService
public class ClassServiceImpl implements ClassService {
  final ClassRepository clsRepo;
  final ProtoMapper mapper;
  @Inject
  public ClassServiceImpl(ClassRepository clsRepo, ProtoMapper mapper) {
    this.clsRepo = clsRepo;
    this.mapper = mapper;
  }

  @Override
  public Uni<ProtoOClass> get(SingleKeyQuery request) {
    return clsRepo.async().getAsync(request.getKey())
      .map(mapper::toProto);
  }

  @Override
  public Multi<ProtoOClass> list(PaginateQuery request) {
    return clsRepo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(page -> Multi.createFrom().iterable(page.items()))
      .map(mapper::toProto);
  }

  @Override
  public Multi<ProtoOClass> select(MultiKeyQuery request) {
    return clsRepo.async()
      .listAsync(request.getKeyList())
      .toMulti()
      .flatMap(l -> Multi.createFrom().iterable(l.values()))
      .map(mapper::toProto);
  }
}
