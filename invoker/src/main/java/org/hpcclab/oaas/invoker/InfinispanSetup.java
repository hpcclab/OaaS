package org.hpcclab.oaas.invoker;

import io.quarkus.cache.CacheManager;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import org.infinispan.api.Infinispan;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class InfinispanSetup {
  @Inject
  Vertx vertx;
  EmbeddedCacheManager cacheManager;
  private static final Logger logger = LoggerFactory.getLogger(InfinispanSetup.class);

  public void setup(
    @Observes StartupEvent event
  ) {
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


    try {
      InputStream configurationStream = FileLookupFactory.newInstance().lookupFileStrict("infinispan.xml",
        Thread.currentThread().getContextClassLoader());
      ConfigurationBuilderHolder configHolder = new ParserRegistry().parse(configurationStream, null, MediaType.APPLICATION_XML);
      configHolder.getGlobalConfigurationBuilder()
          .security();
      logger.info("starting infinispan {}", configHolder);
      cacheManager = new DefaultCacheManager(configHolder, true);
      logger.info("started infinispan");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
