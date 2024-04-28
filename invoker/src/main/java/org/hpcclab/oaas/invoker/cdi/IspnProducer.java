package org.hpcclab.oaas.invoker.cdi;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnObjectRepoManager;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.infinispan.Cache;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.EmbeddedCacheManager;

@ApplicationScoped
@Startup
public class IspnProducer {
  @Produces
  @ApplicationScoped
  ObjectRepoManager objectRepoManager(IspnCacheCreator cacheCreator,
                                      ClassControllerRegistry registry) {
    return new EIspnObjectRepoManager(registry, cacheCreator);
  }


  @Produces
  @ApplicationScoped
  HashRegistry hashRegistry(@GrpcClient("package-manager") InternalCrStateService crStateService,
                            IspnCacheCreator cacheCreator) {
    Cache<String, CrHash> replicatedCache = cacheCreator
      .createReplicateCache("hashRegistry", true, 100000);
    return new HashRegistry(crStateService, replicatedCache);
  }

  @Produces
  @ApplicationScoped
  LookupManager lookupManager(HashRegistry hashRegistry) {
    return new LookupManager(hashRegistry);
  }

  @Produces
  @ApplicationScoped
  ClusteredLockManager clusteredLockManager(EmbeddedCacheManager cacheManager) {
    return EmbeddedClusteredLockManagerFactory.from(cacheManager);
  }
}
