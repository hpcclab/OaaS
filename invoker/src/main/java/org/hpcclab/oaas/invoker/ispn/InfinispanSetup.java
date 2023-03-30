package org.hpcclab.oaas.invoker.ispn;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

@ApplicationScoped
public class InfinispanSetup {
  @Inject
  IspnConfig config;
  EmbeddedCacheManager cacheManager;
  private static final Logger logger = LoggerFactory.getLogger(InfinispanSetup.class);

  public void setup(@Observes StartupEvent event) {
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


    //      InputStream configurationStream = FileLookupFactory.newInstance().lookupFileStrict("infinispan.xml",
//        Thread.currentThread().getContextClassLoader());
//      ConfigurationBuilderHolder configHolder = new ParserRegistry()
//              .parse(configurationStream, ConfigurationResourceResolver.DEFAULT, MediaType.APPLICATION_XML);
//      logger.info("starting infinispan {}", configHolder);
//      cacheManager = new DefaultCacheManager(configHolder, true);
//      logger.info("started infinispan");


    GlobalConfigurationBuilder globalConfigurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
    if (ConfigUtils.getProfiles().contains(LaunchMode.NORMAL.getDefaultProfile())) {
      globalConfigurationBuilder
        .transport()
        .defaultTransport()
        .addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml");
    }

    logger.info("starting infinispan {}", globalConfigurationBuilder);
    cacheManager = new DefaultCacheManager(globalConfigurationBuilder.build());
    logger.info("started infinispan");

    if (config.hotRodPort() >= 0) {
      var hotrod = new HotRodServerConfigurationBuilder()
        .port(config.hotRodPort())
        .build();
      var hotRodServer = new HotRodServer();
      hotRodServer.start(hotrod, cacheManager);
    }
  }

  @Produces
  EmbeddedCacheManager cacheManager() {
    return cacheManager;
  }

  @PreDestroy
  void clean() throws IOException {
    cacheManager.close();
  }
}
