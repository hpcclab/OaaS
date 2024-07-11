package org.hpcclab.oaas.invoker.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;


/**
 * @author Pawissanutt
 */
public class ManagedGrpcInvocationSender implements RemoteInvocationSender {

  final ProtoMapper mapper = new ProtoMapperImpl();
  final GrpcInvocationServicePool pool;
  private static final Logger logger = LoggerFactory.getLogger( ManagedGrpcInvocationSender.class );

  public ManagedGrpcInvocationSender(GrpcInvocationServicePool pool) {
    this.pool = pool;
  }

  public ManagedGrpcInvocationSender(Vertx vertx,
                                     InvokerConfig invokerConfig) {

    this.pool = new GrpcInvocationServicePool(
      invokerConfig.disableVertxForGrpc() ? null:vertx.getDelegate()
    );
  }

  @Override
  public Uni<ProtoInvocationResponse> send(Supplier<CrHash.ApiAddress> addrSupplier,
                                           ProtoInvocationRequest request) {

    Uni<ProtoInvocationResponse> clientResponseUni =
      Uni.createFrom().item(addrSupplier)
        .onItem().ifNull().failWith(HashAwareInvocationHandler.RetryableException::new)
        .map(pool::getOrCreate)
        .flatMap(invocationService -> invocationService.invokeLocal(request));
    return clientResponseUni;
  }

}
