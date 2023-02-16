package org.hpcclab.oaas.invoker;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class InfinispanSetup {
  @Inject
  Vertx vertx;
  private static final Logger logger = LoggerFactory.getLogger( InfinispanSetup.class );
  public void setup(
    @Observes StartupEvent event
  ) {
    // Set up a clustered Cache Manager.
    var dns = System.getenv("INF_DNS_PING");
    if (dns != null) {
      logger.info("use dns.query {}", dns);
      System.setProperty("jgroups.dns.query", dns);
    }

    GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();

    global.transport()
      .defaultTransport()
      .clusterName("oaas-invoker")
      .addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml")
    ;
    // Initialize the default Cache Manager.
    var conf = global.build();
    DefaultCacheManager cacheManager = new DefaultCacheManager(global.build());
    logger.info("starting infinispan {}", conf);
    cacheManager.start();
    logger.info("started infinispan");
    // Create a distributed cache with synchronous replication.
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.clustering().cacheMode(CacheMode.DIST_SYNC);
    // Obtain a volatile cache.
    Cache<String, String> cache = cacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache("myCache", builder.build());
// Stop the Cache Manager.
//    cacheManager.stop();
//    vertx.setPeriodic(5000, l -> {
//      logger.info("cluster size {}", cacheManager.getClusterSize());
//    });
  }
}
