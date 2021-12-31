package org.hpcclab.oaas.repository.init;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InfinispanInit {
  private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanInit.class);
  // language=xml
  private static final String TEMPLATE_DIST_CONFIG = """
    <infinispan>
      <cache-container>
        <distributed-cache name="%s"
                           statistics="true"
                           mode="SYNC">
          <memory storage="OFF_HEAP"
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
            <!--<rocksdb-store xmlns="urn:infinispan:config:store:rocksdb:13.0"
                           fetch-state="true"/> -->
          </persistence>
          <partition-handling when-split="ALLOW_READ_WRITES"
                              merge-policy="PREFERRED_NON_NULL"/>
          <state-transfer timeout='300000'/>
        </distributed-cache>
      </cache-container>
    </infinispan>
     """;
  // language=xml
  private static final String TEMPLATE_REP_CONFIG = """
    <infinispan>
      <cache-container>
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
              </file-store>
          </persistence>
          <partition-handling when-split="ALLOW_READ_WRITES"
                              merge-policy="PREFERRED_NON_NULL"/>
          <state-transfer timeout='300000'/>
        </replicated-cache>
      </cache-container>
    </infinispan>
     """;
  // language=xml
  private static final String TEMPLATE_TX_CONFIG = """
    <infinispan>
      <cache-container>
        <distributed-cache name="%s"
                           statistics="true"
                           mode="SYNC">
          <memory storage="OFF_HEAP"
                  max-size="%s"/>
          <locking isolation="REPEATABLE_READ"/>
          <transaction mode="NON_XA" locking="PESSIMISTIC"/>
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
          <state-transfer timeout='300000'/>
        </distributed-cache>
      </cache-container>
    </infinispan>
     """;

  @Inject
  RemoteCacheManager remoteCacheManager;

  public void setup() {
    if (remoteCacheManager==null) {
      throw new RuntimeException("Cannot connect to infinispan cluster");
    }
    remoteCacheManager.getConfiguration()
      .addRemoteCache("OaasClass", c -> {
        c.nearCacheMode(NearCacheMode.INVALIDATED)
          .nearCacheMaxEntries(1000);
      });
    remoteCacheManager.getConfiguration()
      .addRemoteCache("OaasFunction", c -> {
        c.nearCacheMode(NearCacheMode.INVALIDATED)
          .nearCacheMaxEntries(1000);
      });
//    LOGGER.info("Use hotrod configuration {}", remoteCacheManager.getConfiguration());
    remoteCacheManager.administration().getOrCreateCache("OaasObject", new XMLStringConfiguration(TEMPLATE_DIST_CONFIG
      .formatted("OaasObject", "128MB")));
    remoteCacheManager.administration().getOrCreateCache("OaasClass", new XMLStringConfiguration(TEMPLATE_REP_CONFIG
      .formatted("OaasClass", "16MB")));
    remoteCacheManager.administration().getOrCreateCache("OaasFunction", new XMLStringConfiguration(TEMPLATE_REP_CONFIG
      .formatted("OaasFunction", "16MB")));
    remoteCacheManager.administration().getOrCreateCache("TaskCompletion", new XMLStringConfiguration(TEMPLATE_DIST_CONFIG
      .formatted("TaskCompletion", "128MB")));
    remoteCacheManager.administration().getOrCreateCache("TaskState", new XMLStringConfiguration(TEMPLATE_TX_CONFIG
      .formatted("TaskState", "128MB")));


  }
}
