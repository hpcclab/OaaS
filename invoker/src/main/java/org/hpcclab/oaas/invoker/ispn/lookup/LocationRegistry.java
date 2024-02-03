package org.hpcclab.oaas.invoker.ispn.lookup;


import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.impl.InternalCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jgroups.stack.IpAddress;

public class LocationRegistry {
    Cache<String, ApiAddress> map;
    String localhost;

    public LocationRegistry(Cache<String, ApiAddress> map) {
        this.map = map;
    }

    public void push(String cls, int segment, ApiAddress address){
            map.put(makeKey(cls, segment), address);
    }

    public ApiAddress get(String cls, int segment) {
        return map.get(makeKey(cls, segment));
    }

    String makeKey(String cls, int segment) {
        return cls + "-" + segment;
    }

    public Cache<String, ApiAddress> getMap() {
        return map;
    }

    public void update(String cls, AdvancedCache<?,?> cache, String host, int port) {
        var topology = cache.getAdvancedCache()
                .getDistributionManager()
                .getCacheTopology();
        for (Integer segment : topology.getLocalPrimarySegments()) {
            push(cls, segment, new ApiAddress(host, port));
        }
    }

    public void initLocal() {
      if (localhost == null) {
        var registry = InternalCacheManager.of(map.getCacheManager());
        var transport = registry.getComponent(Transport.class);
        JGroupsAddress address = (JGroupsAddress) transport.getPhysicalAddresses().get(0);
        IpAddress ipAddress = (IpAddress) address.getJGroupsAddress();
        localhost = ipAddress.getIpAddress().getHostAddress();
      }
    }

  public String getLocalhost() {
    return localhost;
  }
}
