package org.hpcclab.oaas.orbit;

import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.orbit.controller.OrbitController;
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
  Map<Long, OrbitController> orbitMap = ConcurrentHashMap.newMap();

  public OrbitController get(long id) {
    return orbitMap.get(id);
  }

  public OrbitController getOrLoad(long id, Supplier<OrbitController> supplier) {
    var orbit = orbitMap.get(id);
    if (orbit==null) {
      orbit = supplier.get();
      save(orbit);
    }
    return orbit;
  }

  public void save(OrbitController orbit) {
    orbitMap.put(orbit.getId(), orbit);
  }
}
