package org.hpcclab.oaas.initializer;

import io.quarkus.runtime.StartupEvent;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class InfinispanInit {

  private static final String TEMPLATE_CONFIG = """
            <infinispan>
             <cache-container>
              <distributed-cache name="%s"
                                 statistics="true">
                <memory storage="OFF_HEAP"/>
                <encoding>
                    <key media-type="text/plain; charset=UTF-8"/>
                    <value media-type="application/x-protostream"/>
                </encoding>
                <persistence passivation="false">
                    <file-store fetch-state="true">
                      <index path="index" />
                      <data path="data" />
                    </file-store>
                </persistence>
              </distributed-cache>
            </cache-container></infinispan>
             """;
  @Inject
  RemoteCacheManager remoteCacheManager;

  public void setup() {
    remoteCacheManager.administration().getOrCreateCache("OaasObject", new XMLStringConfiguration(TEMPLATE_CONFIG.formatted("OaasObject")));
    remoteCacheManager.administration().getOrCreateCache("OaasClass", new XMLStringConfiguration(TEMPLATE_CONFIG.formatted("OaasClass")));
    remoteCacheManager.administration().getOrCreateCache("OaasFunction", new XMLStringConfiguration(TEMPLATE_CONFIG.formatted("OaasFunction")));
  }
}
