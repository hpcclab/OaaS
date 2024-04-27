package org.hpcclab.oaas.invoker.service;

import io.grpc.ManagedChannelBuilder;
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
  Map<ServiceAddr, InvocationService> invocationServiceMap = new ConcurrentHashMap<>();



  public InvocationService getOrCreate(CrHash.ApiAddress addr) {
    ServiceAddr serviceAddr = new ServiceAddr(addr.host(), addr.port());
    return invocationServiceMap.computeIfAbsent(serviceAddr, k -> {
      ManagedChannelBuilder<?> builder = ManagedChannelBuilder
        .forAddress(addr.host(), addr.port())
        .disableRetry()
        .usePlaintext()
        .directExecutor();
      return new InvocationServiceClient(addr.toString(), builder.build(), this::configure);
    });
  }

  MutinyInvocationServiceStub configure(String key, MutinyInvocationServiceStub stub) {
    return stub;
  }

  record ServiceAddr(String host, int port) {

  }
}
