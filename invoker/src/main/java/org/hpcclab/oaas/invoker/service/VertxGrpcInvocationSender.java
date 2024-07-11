package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcStatus;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author Pawissanutt
 */
public class VertxGrpcInvocationSender implements RemoteInvocationSender {
  private static final Logger logger = LoggerFactory.getLogger(VertxGrpcInvocationSender.class);
  final GrpcClient grpcClient;
  final ProtoMapper mapper = new ProtoMapperImpl();

  public VertxGrpcInvocationSender(GrpcClient grpcClient) {
    this.grpcClient = grpcClient;
  }

  public static SocketAddress toSocketAddress(CrHash.ApiAddress address) {
    return SocketAddress.inetSocketAddress(address.port(), address.host());
  }

  @Override
  public Uni<ProtoInvocationResponse> send(Supplier<CrHash.ApiAddress> addrSupplier,
                                           ProtoInvocationRequest request) {

    Uni<ProtoInvocationResponse> clientResponseUni =
      Uni.createFrom().item(addrSupplier)
        .onItem().ifNull().failWith(HashAwareInvocationHandler.RetryableException::new)
        .flatMap(addr -> callGrpc(request, addr)
        );
    return clientResponseUni;
  }

  private Uni<ProtoInvocationResponse> callGrpc(ProtoInvocationRequest request,
                                                CrHash.ApiAddress addr) {
    MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeLocalMethod();
    return Uni.createFrom().emitter(emitter ->
      grpcClient.request(toSocketAddress(addr), invokeMethod)
        .compose(grpcClientRequest -> grpcClientRequest
          .exceptionHandler(emitter::fail)
          .end(request)
          .compose(__ -> {
            logger.debug("done end to {}", addr);
            return grpcClientRequest.response();
          }))
        .compose(resp -> {
          logger.debug("handling resp from {}", addr);
          if (resp.status()==GrpcStatus.UNAVAILABLE ||
            resp.status()==GrpcStatus.UNKNOWN)
            return Future.failedFuture(new HashAwareInvocationHandler.RetryableException());
          resp.errorHandler(err -> emitter.fail(StdOaasException.format("grpc error %s", err.toString())));
          resp.exceptionHandler(err -> emitter.fail(new StdOaasException("grpc error", err)));
          return resp.last();
        })
        .onSuccess(emitter::complete)
        .onFailure(emitter::fail)
    );

  }
}
