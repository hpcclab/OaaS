package org.hpcclab.oaas.crm;

import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.proto.CrStateUpdaterGrpc;
import org.hpcclab.oaas.proto.OrbitStateServiceGrpc;

import java.util.Map;
import java.util.function.Supplier;

@ApplicationScoped
public class CrRepository {
  @GrpcClient("package-manager")
  CrStateUpdaterGrpc.CrStateUpdaterBlockingStub orbitStateUpdater;
  @GrpcClient("package-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub orbitStateService;
  Map<Long, CrController> orbitMap = ConcurrentHashMap.newMap();

  public CrController get(long id) {
    return orbitMap.get(id);
  }

  public CrController getOrLoad(long id, Supplier<CrController> supplier) {
    var orbit = orbitMap.get(id);
    if (orbit==null) {
      orbit = supplier.get();
      save(orbit);
    }
    return orbit;
  }

  public void save(CrController orbit) {
    orbitMap.put(orbit.getId(), orbit);
  }
}
