package org.hpcclab.oaas.invoker.service;

import io.grpc.ManagedChannelBuilder;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InvocationService;
import org.hpcclab.oaas.proto.InvocationServiceClient;
import org.hpcclab.oaas.proto.MutinyInvocationServiceGrpc.MutinyInvocationServiceStub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pawissanutt
 */
public class GrpcInvocationServicePool {
  Map<CrHash.ApiAddress, InvocationService> invocationServiceMap = new ConcurrentHashMap<>();



  public InvocationService getOrCreate(CrHash.ApiAddress addr) {
    CrHash.ApiAddress noTsAddr = addr.toBuilder().ts(0).build();
    return invocationServiceMap.computeIfAbsent(noTsAddr, k -> {
      ManagedChannelBuilder<?> builder = ManagedChannelBuilder
        .forAddress(addr.host(), addr.port())
        .disableRetry()
        .usePlaintext();
      return new InvocationServiceClient(addr.toString(), builder.build(), this::configure);
    });
  }

  MutinyInvocationServiceStub configure(String key, MutinyInvocationServiceStub stub) {
    return stub;
  }
}
