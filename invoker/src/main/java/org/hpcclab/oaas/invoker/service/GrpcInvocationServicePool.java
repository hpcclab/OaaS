package org.hpcclab.oaas.invoker.service;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InvocationService;
import org.hpcclab.oaas.proto.InvocationServiceClient;
import org.hpcclab.oaas.proto.MutinyInvocationServiceGrpc.MutinyInvocationServiceStub;

import java.util.Map;

/**
 * @author Pawissanutt
 */
public class GrpcInvocationServicePool {
  final Vertx vertx;
  Map<ServiceAddr, InvocationService> invocationServiceMap = new ConcurrentHashMap<>();

  public GrpcInvocationServicePool(Vertx vertx) {
    this.vertx = vertx;
  }

  private Channel createChanel(ServiceAddr addr) {
    try {
      if (vertx!=null) {
        VertxChannelBuilder vertxChannelBuilder = VertxChannelBuilder.forAddress(vertx, addr.host, addr.port)
          .disableRetry()
          .usePlaintext();
        return vertxChannelBuilder.build();
      } else {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
          .forAddress(addr.host(), addr.port())
          .disableRetry()
          .usePlaintext()
          .directExecutor();
        return builder.build();
      }
    } catch (RuntimeException e) {
      throw new HashAwareInvocationHandler.RetryableException(e);
    }

  }

  public InvocationService getOrCreate(CrHash.ApiAddress addr) {
    ServiceAddr serviceAddr = new ServiceAddr(addr.host(), addr.port());
    return invocationServiceMap.computeIfAbsent(serviceAddr,
      k -> new InvocationServiceClient(addr.toString(), createChanel(serviceAddr), this::configure));
  }

  MutinyInvocationServiceStub configure(String key, MutinyInvocationServiceStub stub) {
    return stub;
  }

  record ServiceAddr(String host, int port) {

  }
}
