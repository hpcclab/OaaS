package org.hpcclab.oaas.invoker.lookup;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.hpcclab.oaas.proto.PaginateQuery;
import org.hpcclab.oaas.proto.ProtoApiAddress;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.IntSet;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.impl.InternalCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jgroups.stack.IpAddress;

import java.util.Map;

/**
 * @author Pawissanutt
 */
public class HashRegistry {

  InternalCrStateService crStateService;
  Map<String, ProtoCrHash> cacheMap = new ConcurrentHashMap<>();
  String localAdvertiseAddress;

  public HashRegistry(InternalCrStateService crStateService) {
    this.crStateService = crStateService;
  }

  public Uni<ProtoCrHash> push(String cls, int totalSegment, IntSet segments, ProtoApiAddress address) {
    var builder = ProtoCrHash.newBuilder()
      .setCls(cls)
      .setNumSegment(totalSegment);
    var nullAddress = ProtoApiAddress.newBuilder().setPort(-1).build();
    for (int i = 0; i < totalSegment; i++) {
      if (segments.contains(i)) {
        builder
          .addSegmentAddr(address);
      } else {
        builder.addSegmentAddr(nullAddress);
      }
    }
    ProtoCrHash protoCrHash = builder.build();
    return crStateService.updateHash(protoCrHash)
      .invoke(h -> cacheMap.put(cls, protoCrHash));
  }

  public Uni<Void> warmCache() {
    return crStateService.listHash(PaginateQuery.newBuilder().setLimit(1000).setOffset(0).build())
      .invoke(hash -> {
        cacheMap.put(hash.getCls(), hash);
      })
      .collect().last().replaceWithVoid();
  }

  public ProtoApiAddress get(String cls, int segment) {
    ProtoCrHash protoCrHash = cacheMap.get(cls);
    if (protoCrHash==null) return null;
    return protoCrHash.getSegmentAddr(segment);
  }
  public Uni<Void> updateLocal(String cls, AdvancedCache<?, ?> cache, int port) {
    var topology = cache.getAdvancedCache()
      .getDistributionManager()
      .getCacheTopology();
    return push(cls,
      topology.getNumSegments(),
      topology.getLocalPrimarySegments(),
      ProtoApiAddress.newBuilder().setPort(port).setHost(localAdvertiseAddress).build())
      .replaceWithVoid();
  }
  public Map<String, ProtoCrHash> getMap() {
    return cacheMap;
  }


  public void initLocal(EmbeddedCacheManager cacheManager) {
    if (localAdvertiseAddress==null) {
      var registry = InternalCacheManager.of(cacheManager);
      var transport = registry.getComponent(Transport.class);
      JGroupsAddress address = (JGroupsAddress) transport.getPhysicalAddresses().get(0);
      IpAddress ipAddress = (IpAddress) address.getJGroupsAddress();
      localAdvertiseAddress = ipAddress.getIpAddress().getHostAddress();
    }
  }

  public String getLocalAdvertiseAddress() {
    return localAdvertiseAddress;
  }
}
