package org.hpcclab.oaas.invoker;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import org.infinispan.api.Infinispan;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class InfinispanSetup {
  @Inject
  Vertx vertx;
  private static final Logger logger = LoggerFactory.getLogger(InfinispanSetup.class);

  public void setup(
    @Observes StartupEvent event
  ) {
    // Set up a clustered Cache Manager.
    var dns = System.getenv("ISPN_DNS_PING");
    if (dns!=null) {
      logger.info("use dns.query {}", dns);
//      System.setProperty("infinispan.cluster.stack", "kubernetes");
      System.setProperty("jgroups.dns.query", dns);
    }
    var podName = System.getenv("ISPN_POD_NAME");
    if (podName==null) {
      podName = "invoker-" + UUID.randomUUID();
    }
    logger.info("use nodeName {}", podName);


    GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();

    global.defaultCacheName("defaultCache")
      .transport()
      .defaultTransport()
      .clusterName("oaas-invoker")
      .nodeName(podName)
      .addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml")
    ;

    Configuration cacheConfiguration = new ConfigurationBuilder()
      .clustering()
      .cacheMode(CacheMode.REPL_SYNC)
      .build();

    // Initialize the default Cache Manager.
    var conf = global.build();
    DefaultCacheManager cacheManager = new DefaultCacheManager(global.build(), cacheConfiguration);
    logger.info("starting infinispan {}", conf);
    cacheManager.start();
    logger.info("started infinispan");
  }
}
