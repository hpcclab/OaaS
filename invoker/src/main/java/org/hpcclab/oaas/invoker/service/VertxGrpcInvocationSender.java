package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcStatus;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler.RetryableException;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
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

  private static Future<ProtoInvocationResponse> mapError(GrpcClientResponse<ProtoInvocationRequest, ProtoInvocationResponse> resp) {
    if (resp.status()==GrpcStatus.UNAVAILABLE ||
      resp.status()==GrpcStatus.UNKNOWN)
      return Future.failedFuture(new RetryableException());
    else if (resp.status()==GrpcStatus.INTERNAL) {
      logger.warn("grpc internal error: {}", resp.statusMessage());
      return Future.failedFuture(new RetryableException());
    } else if (resp.status()==GrpcStatus.RESOURCE_EXHAUSTED)
      return Future.failedFuture(new StdOaasException(resp.statusMessage(),
        HttpResponseStatus.TOO_MANY_REQUESTS.code()));
    else if (resp.status()==GrpcStatus.NOT_FOUND)
      return Future.failedFuture(new StdOaasException(resp.statusMessage(), 404));
    else if (resp.status()==GrpcStatus.UNIMPLEMENTED)
      return Future.failedFuture(new StdOaasException(resp.statusMessage(),
        HttpResponseStatus.NOT_IMPLEMENTED.code()));
    else if (resp.status()==GrpcStatus.INVALID_ARGUMENT)
      return Future.failedFuture(new StdOaasException(resp.statusMessage(),
        HttpResponseStatus.BAD_REQUEST.code()));

    return null;
  }

  @Override
  public Uni<ProtoInvocationResponse> send(Supplier<CrHash.ApiAddress> addrSupplier,
                                           ProtoInvocationRequest request) {

    return Uni.createFrom().item(addrSupplier)
      .onItem().ifNull().failWith(RetryableException::new)
      .flatMap(addr -> callGrpc(request, addr)
      );
  }

  private Uni<ProtoInvocationResponse> callGrpc(ProtoInvocationRequest request,
                                                CrHash.ApiAddress addr) {
    MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeLocalMethod();
    return Uni.createFrom().<ProtoInvocationResponse>emitter(emitter ->
        grpcClient.request(toSocketAddress(addr), invokeMethod)
          .compose(grpcClientRequest -> grpcClientRequest
            .exceptionHandler(emitter::fail)
            .end(request)
            .compose(__ -> grpcClientRequest.response()))
          .compose(resp -> {
            logger.debug("handling resp from {}", addr);
            Future<ProtoInvocationResponse> failedFuture = mapError(resp);
            if (failedFuture!=null) return failedFuture;
            resp.errorHandler(err -> emitter.fail(StdOaasException.format("grpc error %s", err.toString())));
            resp.exceptionHandler(err -> emitter.fail(new StdOaasException("grpc error", err)));
            return resp.last();
          })
          .onSuccess(emitter::complete)
          .onFailure(emitter::fail)
      )
      .onFailure(ConnectException.class).transform(RetryableException::new)
      .onFailure(ConnectTimeoutException.class).transform(RetryableException::new)
      .onFailure(HttpClosedException.class).transform(RetryableException::new)
      .onFailure(VertxException.class).transform(RetryableException::new)
      .onFailure(e -> !(e instanceof RetryableException))
      .invoke(throwable -> logger.warn("grpc error ", throwable));
  }
}
