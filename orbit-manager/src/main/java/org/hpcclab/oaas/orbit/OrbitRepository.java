package org.hpcclab.oaas.orbit;

import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.proto.OrbitStateServiceGrpc;
import org.hpcclab.oaas.proto.OrbitStateUpdaterGrpc;

import java.util.Map;
import java.util.function.Supplier;

@ApplicationScoped
public class OrbitRepository {
  @GrpcClient("class-manager")
  OrbitStateUpdaterGrpc.OrbitStateUpdaterBlockingStub orbitStateUpdater;
  @GrpcClient("class-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub orbitStateService;
  Map<Long, OrbitStructure> orbitMap = ConcurrentHashMap.newMap();

  public OrbitStructure get(long id) {
    return orbitMap.get(id);
  }

  public OrbitStructure getOrLoad(long id, Supplier<OrbitStructure> supplier) {
    var orbit = orbitMap.get(id);
    if (orbit==null) {
      orbit = supplier.get();
      save(orbit);
    }
    return orbit;
  }

  public void save(OrbitStructure orbit) {
    orbitMap.put(orbit.getId(), orbit);
  }
}
