package org.hpcclab.oaas.controller.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.proto.ClassService;
import org.hpcclab.oaas.proto.PaginateQuery;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.SingleKeyQuery;
import org.hpcclab.oaas.repository.ClassRepository;

@ApplicationScoped
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
    return clsRepo.getQueryService().paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(page -> Multi.createFrom().iterable(page.getItems()))
      .map(mapper::toProto);
  }
}
