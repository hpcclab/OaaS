package org.hpcclab.oaas.invoker.cdi;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.invoker.ispn.IspnConfig;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnObjectRepoManager;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  HashRegistry hashRegistry(@GrpcClient("package-manager") InternalCrStateService crStateService) {
    return new HashRegistry(crStateService);
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
