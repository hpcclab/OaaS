package org.hpcclab.oaas.invoker.ispn;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationScoped
public class IspnInitializer {
  private static final Logger logger = LoggerFactory.getLogger(IspnInitializer.class);
  final IspnConfig config;
  EmbeddedCacheManager cacheManager;

  @Inject
  public IspnInitializer(IspnConfig config) {
    this.config = config;
  }

  public EmbeddedCacheManager setup() {
    // Set up a clustered Cache Manager.
    var dns = System.getenv("ISPN_DNS_PING");
    if (dns!=null) {
      logger.info("use dns.query {}", dns);
      System.setProperty("jgroups.dns.query", dns);
    }
    var podName = System.getenv("ISPN_POD_NAME");
    if (podName==null) {
      podName = "invoker-" + UUID.randomUUID();
    }
    System.setProperty("infinispan.node.name", podName);


    GlobalConfigurationBuilder globalConfigurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
    if (dns!=null) {
      globalConfigurationBuilder
        .transport()
        .defaultTransport()
        .addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml");
    }
    globalConfigurationBuilder.transport().nodeName(podName);
    globalConfigurationBuilder.globalState()
      .persistentLocation("ispn")
      .configurationStorage(ConfigurationStorage.VOLATILE)
      .enable();

    logger.info("starting infinispan {}", globalConfigurationBuilder);
    cacheManager = new DefaultCacheManager(globalConfigurationBuilder.build());
    logger.info("started infinispan");

//    if (config.hotRodPort() >= 0) {
//      var hotrod = new HotRodServerConfigurationBuilder()
//        .port(config.hotRodPort())
//        .build();
//      var hotRodServer = new HotRodServer();
//      hotRodServer.start(hotrod, cacheManager);
//    }
    return cacheManager;
  }

  @Produces
  @Alternative
  @Priority(100)
  @ApplicationScoped
  EmbeddedCacheManager embeddedCacheManager() {
    if (cacheManager==null)
      cacheManager = setup();
    return cacheManager;
  }

  void clean(@Observes ShutdownEvent event) {
    logger.info("Stopping infinispan...");
    cacheManager.stop();
  }
}
