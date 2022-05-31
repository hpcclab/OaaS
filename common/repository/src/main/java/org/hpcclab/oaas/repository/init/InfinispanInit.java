package org.hpcclab.oaas.repository.init;

import org.hpcclab.oaas.model.exception.NoStackException;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.multimap.MultimapCacheManager;
import org.infinispan.client.hotrod.multimap.RemoteMultimapCache;
import org.infinispan.client.hotrod.multimap.RemoteMultimapCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class InfinispanInit {
  private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanInit.class);
  public static final String OBJECT_CACHE = "OaasObject";
  public static final String CLASS_CACHE = "OaasClass";
  public static final String FUNCTION_CACHE = "OaasFunction";

  public static final String INVOCATION_GRAPH_CACHE = "InvocationGraph";

  // language=xml
  private static final String TEMPLATE_MEM_DIST_CONFIG = """
    <distributed-cache name="%s"
                       statistics="true"
                       mode="ASYNC">
      <indexing storage="local-heap">
        <indexed-entities>
          <indexed-entity>oaas.OaasObject</indexed-entity>
        </indexed-entities>
      </indexing>
      <memory storage="OFF_HEAP"
              max-size="%s"/>
      <encoding>
          <key media-type="application/x-protostream"/>
          <value media-type="application/x-protostream"/>
      </encoding>
      <partition-handling when-split="ALLOW_READ_WRITES"
                          merge-policy="PREFERRED_NON_NULL"/>
      <state-transfer timeout="300000"/>
    </distributed-cache>
    """;

  // language=xml
  private static final String TEMPLATE_DIST_CONFIG = """
    <distributed-cache name="%s"
                       statistics="true"
                       mode="ASYNC">
      <indexing>
        <indexed-entities>
          <indexed-entity>oaas.OaasObject</indexed-entity>
        </indexed-entities>
      </indexing>
      <memory storage="OFF_HEAP"
              max-size="%s"/>
      <encoding>
          <key media-type="application/x-protostream"/>
          <value media-type="application/x-protostream"/>
      </encoding>
      <persistence passivation="false">
        <rocksdb-store xmlns="urn:infinispan:config:store:rocksdb:13.0"
                       fetch-state="true">
          <write-behind modification-queue-size="%d"/>
        </rocksdb-store>
      </persistence>
      <partition-handling when-split="ALLOW_READ_WRITES"
                          merge-policy="PREFERRED_NON_NULL"/>
      <state-transfer timeout="300000"/>
    </distributed-cache>
    """;
  // language=xml
  private static final String TEMPLATE_REP_CONFIG = """
    <replicated-cache name="%s"
                      statistics="true"
                      mode="SYNC">
      <memory storage="HEAP"
              max-size="%s"/>
      <encoding>
          <key media-type="application/x-protostream"/>
          <value media-type="application/x-protostream"/>
      </encoding>
      <persistence passivation="false">
        <file-store shared="false"
                    fetch-state="true"
                    purge="false"
                    preload="false">
          <!--<write-behind modification-queue-size="65536" />-->
        </file-store>
      </persistence>
      <partition-handling when-split="ALLOW_READ_WRITES"
                          merge-policy="PREFERRED_NON_NULL"/>
      <state-transfer timeout="300000"/>
    </replicated-cache>
    """;
  // language=xml
  private static final String TEMPLATE_TX_CONFIG = """
   <distributed-cache name="%s"
                      statistics="true"
                      mode="SYNC">
     <memory storage="OFF_HEAP"
             max-size="%s"/>
     <locking isolation="REPEATABLE_READ"/>
     <transaction mode="NON_XA"
                  locking="PESSIMISTIC"/>
      <!--  locking="PESSIMISTIC"-->
      <!--  locking="OPTIMISTIC"-->
     <encoding>
         <key media-type="application/x-protostream"/>
         <value media-type="application/x-protostream"/>
     </encoding>
     <persistence passivation="false">
         <file-store shared="false"
                     fetch-state="true"
                     purge="false"
                     preload="false">
         <!--<write-behind modification-queue-size="65536" />-->
         </file-store>
     </persistence>
     <partition-handling when-split="ALLOW_READ_WRITES"
                         merge-policy="PREFERRED_NON_NULL"/>
     <state-transfer timeout="300000"/>
   </distributed-cache>
    """;

  // language=xml
  private static final String TEMPLATE_MULTIMAP_CONFIG = """
    <distributed-cache name="%s"
                       statistics="true"
                       mode="ASYNC">
      <memory storage="HEAP"
              max-size="%s"/>
      <encoding>
          <key media-type="application/x-protostream"/>
          <value media-type="application/x-protostream"/>
      </encoding>
      <partition-handling when-split="ALLOW_READ_WRITES"
                          merge-policy="PREFERRED_NON_NULL"/>
      <state-transfer timeout="300000"/>
    </distributed-cache>
    """;

  @Inject
  RemoteCacheManager remoteCacheManager;
  @Inject
  RepositoryConfig repositoryConfig;

  public void setup() {
    if (remoteCacheManager==null) {
      throw new RuntimeException("Cannot connect to infinispan cluster");
    }
    var objectConfig = repositoryConfig.object();
    var graphConfig = repositoryConfig.graph();
    var clsCacheConfig = repositoryConfig.cls();
    var funcCacheConfig = repositoryConfig.func();
    remoteCacheManager.getConfiguration()
      .addRemoteCache(OBJECT_CACHE, c -> {
        if (objectConfig.nearCacheMaxEntry() > 0) {
          c.nearCacheMode(NearCacheMode.INVALIDATED)
            .nearCacheMaxEntries(objectConfig.nearCacheMaxEntry());
        }
        c.forceReturnValues(false);
      });
    remoteCacheManager.getConfiguration()
      .addRemoteCache(CLASS_CACHE, c -> {
        if (clsCacheConfig.nearCacheMaxEntry() > 0) {
          c.nearCacheMode(NearCacheMode.INVALIDATED)
            .nearCacheMaxEntries(clsCacheConfig.nearCacheMaxEntry());
        }
      });
    remoteCacheManager.getConfiguration()
      .addRemoteCache(FUNCTION_CACHE, c -> {
        if (funcCacheConfig.nearCacheMaxEntry() > 0) {
          c.nearCacheMode(NearCacheMode.INVALIDATED)
            .nearCacheMaxEntries(funcCacheConfig.nearCacheMaxEntry());
        }
      });


    if (repositoryConfig.createOnStart()) {
      var distTemplate = objectConfig.persist() ?
        TEMPLATE_DIST_CONFIG:TEMPLATE_MEM_DIST_CONFIG;

      remoteCacheManager.administration().getOrCreateCache(INVOCATION_GRAPH_CACHE, new XMLStringConfiguration(TEMPLATE_MULTIMAP_CONFIG
        .formatted(INVOCATION_GRAPH_CACHE, graphConfig.maxSize())));

      remoteCacheManager.administration().getOrCreateCache(CLASS_CACHE, new XMLStringConfiguration(TEMPLATE_REP_CONFIG
        .formatted(CLASS_CACHE, "16MB")));

      remoteCacheManager.administration().getOrCreateCache(FUNCTION_CACHE, new XMLStringConfiguration(TEMPLATE_REP_CONFIG
        .formatted(FUNCTION_CACHE, "16MB")));

      remoteCacheManager.administration().getOrCreateCache(OBJECT_CACHE, new XMLStringConfiguration(distTemplate
        .formatted(OBJECT_CACHE,
          objectConfig.maxSize(),
          objectConfig.writeBackQueueSize())));

    } else {
      var list = List.of(
        OBJECT_CACHE,
        CLASS_CACHE,
        FUNCTION_CACHE,
        INVOCATION_GRAPH_CACHE);
      for (String cacheName : list) {
        if (remoteCacheManager.getCache(cacheName) == null)
          throw new IllegalStateException("Cache '%s' is not ready".formatted(cacheName));
      }
    }
  }

  @Produces
  public RemoteMultimapCache<String,String> graph() {
    var rmcm = new RemoteMultimapCacheManager<String,String>(remoteCacheManager);
    return rmcm.get(INVOCATION_GRAPH_CACHE);
  }
}
