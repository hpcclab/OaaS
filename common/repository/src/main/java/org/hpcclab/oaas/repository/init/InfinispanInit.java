package org.hpcclab.oaas.repository.init;

import org.hpcclab.oaas.model.exception.NoStackException;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class InfinispanInit {
  private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanInit.class);
  public static final String OBJECT_CACHE = "OaasObject";
  public static final String CLASS_CACHE = "OaasClass";
  public static final String FUNCTION_CACHE = "OaasFunction";
  public static final String TASK_STATE_CACHE = "TaskState";
  public static final String TASK_COMPLETION_CACHE = "TaskCompletion";

  // language=xml
  private static final String TEMPLATE_MEM_DIST_CONFIG = """
    <distributed-cache name="%s"
                       statistics="true"
                       mode="ASYNC">
      <indexing storage="local-heap">
        <indexed-entities>
          <indexed-entity>org.hpcclab.oaas.model.proto.OaasObject</indexed-entity>
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
                       mode="SYNC">
      <indexing>
        <indexed-entities>
          <indexed-entity>org.hpcclab.oaas.model.proto.OaasObject</indexed-entity>
        </indexed-entities>
      </indexing>
      <memory storage="OFF_HEAP"
              max-size="%s"/>
      <encoding>
          <key media-type="application/x-protostream"/>
          <value media-type="application/x-protostream"/>
      </encoding>
      <persistence passivation="false">
        <!--<file-store shared="false"
                    fetch-state="true"
                    purge="false"
                    preload="false">
          <write-behind modification-queue-size="65536" />
        </file-store>-->
        <rocksdb-store xmlns="urn:infinispan:config:store:rocksdb:13.0"
                       fetch-state="true"/>
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
        <!--
        <rocksdb-store xmlns="urn:infinispan:config:store:rocksdb:13.0"
                       fetch-state="true"/>-->
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

  @Inject
  RemoteCacheManager remoteCacheManager;
  @Inject
  RepositoryConfig repositoryConfig;

  public void setup() {
    if (remoteCacheManager==null) {
      throw new RuntimeException("Cannot connect to infinispan cluster");
    }
    var objectCacheConfig = repositoryConfig.object();
    var completionCacheConfig = repositoryConfig.completion();
    var stateCacheConfig = repositoryConfig.state();
    var clsCacheConfig = repositoryConfig.cls();
    var funcCacheConfig = repositoryConfig.func();
    remoteCacheManager.getConfiguration()
      .addRemoteCache(OBJECT_CACHE, c -> {
        if (objectCacheConfig.nearCacheMaxEntry() > 0) {
          c.nearCacheMode(NearCacheMode.INVALIDATED)
            .nearCacheMaxEntries(objectCacheConfig.nearCacheMaxEntry());
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
    remoteCacheManager.getConfiguration()
      .addRemoteCache(TASK_COMPLETION_CACHE, c -> {
        if (completionCacheConfig.nearCacheMaxEntry() > 0) {
          c.nearCacheMode(NearCacheMode.INVALIDATED)
            .nearCacheMaxEntries(completionCacheConfig.nearCacheMaxEntry());
        }
        c.forceReturnValues(false);
      });
    remoteCacheManager.getConfiguration()
      .addRemoteCache(TASK_STATE_CACHE, c -> {
        if (stateCacheConfig.nearCacheMaxEntry() > 0) {
          c.nearCacheMode(NearCacheMode.INVALIDATED)
            .nearCacheMaxEntries(stateCacheConfig.nearCacheMaxEntry());
        }
        c.forceReturnValues(false);
      });

    if (repositoryConfig.createOnStart()) {
      var distTemplate = objectCacheConfig.persist() ?
        TEMPLATE_DIST_CONFIG:TEMPLATE_MEM_DIST_CONFIG;
      remoteCacheManager.administration().getOrCreateCache(OBJECT_CACHE, new XMLStringConfiguration(distTemplate
        .formatted(OBJECT_CACHE, objectCacheConfig.maxSize())));
      remoteCacheManager.administration().getOrCreateCache(CLASS_CACHE, new XMLStringConfiguration(TEMPLATE_REP_CONFIG
        .formatted(CLASS_CACHE, "16MB")));
      remoteCacheManager.administration().getOrCreateCache(FUNCTION_CACHE, new XMLStringConfiguration(TEMPLATE_REP_CONFIG
        .formatted(FUNCTION_CACHE, "16MB")));

      distTemplate = completionCacheConfig.persist() ?
        TEMPLATE_DIST_CONFIG:TEMPLATE_MEM_DIST_CONFIG;
      remoteCacheManager.administration().getOrCreateCache(TASK_COMPLETION_CACHE, new XMLStringConfiguration(distTemplate
        .formatted(TASK_COMPLETION_CACHE, completionCacheConfig.maxSize())));

      distTemplate = stateCacheConfig.persist() ?
        TEMPLATE_DIST_CONFIG:TEMPLATE_MEM_DIST_CONFIG;
      remoteCacheManager.administration().getOrCreateCache(TASK_STATE_CACHE, new XMLStringConfiguration(distTemplate
        .formatted(TASK_STATE_CACHE, stateCacheConfig.maxSize())));
    } else {
      var list = List.of(OBJECT_CACHE,CLASS_CACHE,FUNCTION_CACHE,
        TASK_STATE_CACHE, TASK_COMPLETION_CACHE);
      for (String cacheName : list) {
        if (remoteCacheManager.getCache(cacheName) == null)
          throw new RuntimeException("Cache '%s' is not ready".formatted(cacheName));
      }
    }
  }
}
