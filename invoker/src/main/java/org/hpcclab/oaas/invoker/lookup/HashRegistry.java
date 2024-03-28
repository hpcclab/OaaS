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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class HashRegistry {
  private static final Logger logger = LoggerFactory.getLogger(HashRegistry.class);
  private static final ProtoApiAddress NULL = ProtoApiAddress.newBuilder().setPort(-1).build();
  final InternalCrStateService crStateService;
  final Map<String, ProtoCrHash> cacheMap = new ConcurrentHashMap<>();
  String localAdvertiseAddress;

  public HashRegistry(InternalCrStateService crStateService) {
    this.crStateService = crStateService;
  }

  public Uni<ProtoCrHash> push(String cls, int totalSegment, IntSet segments, ProtoApiAddress address) {
    var current = cacheMap.getOrDefault(cls, ProtoCrHash.getDefaultInstance());
    var builder = ProtoCrHash.newBuilder()
      .setCls(cls)
      .setNumSegment(totalSegment);
    for (int i = 0; i < totalSegment; i++) {
      if (segments.contains(i)) {
        builder
          .addSegmentAddr(address);
      } else {
        var old = i < current.getSegmentAddrCount() ? current.getSegmentAddr(i): NULL;
        builder.addSegmentAddr(old);
      }
    }
    ProtoCrHash protoCrHash = builder
      .setTs(System.currentTimeMillis())
      .build();
    return crStateService.updateHash(protoCrHash)
      .invoke(h -> cacheMap.put(cls, protoCrHash));
  }

  public Uni<Void> warmCache() {
    return crStateService.listHash(PaginateQuery.newBuilder().setLimit(1000).setOffset(0).build())
      .invoke(this::updateManaged)
      .collect().last().replaceWithVoid();
  }

  public ProtoApiAddress get(String cls, int segment) {
    ProtoCrHash protoCrHash = cacheMap.get(cls);
    if (protoCrHash==null) return null;
    return protoCrHash.getSegmentAddr(segment);
  }

  public Uni<Void> updateManaged(String cls, AdvancedCache<?, ?> cache, int port) {
    var topology = cache.getAdvancedCache()
      .getDistributionManager()
      .getCacheTopology();
    return push(cls,
      topology.getNumSegments(),
      topology.getLocalPrimarySegments(),
      ProtoApiAddress.newBuilder()
        .setPort(port)
        .setTs(System.currentTimeMillis())
        .setHost(localAdvertiseAddress).build()
    ).replaceWithVoid();
  }

  public void updateManaged(ProtoCrHash protoCrHash) {
    logger.debug("update local hash registry '{}'", protoCrHash.getCls());
    cacheMap.compute(protoCrHash.getCls(), (k, v) -> {
      if (v==null) return protoCrHash;
      return merge(v, protoCrHash);
    });
  }

  static ProtoCrHash merge (ProtoCrHash hash1, ProtoCrHash hash2) {
    var count = hash1.getTs() > hash2.getTs() ? hash1.getNumSegment(): hash2.getNumSegment();
    List<ProtoApiAddress> list = new ArrayList<>(count);
    List<ProtoApiAddress> h1List = hash1.getSegmentAddrList();
    List<ProtoApiAddress> h2List = hash2.getSegmentAddrList();
    for (int i = 0; i < count; i++) {
      ProtoApiAddress addr1 = h1List.get(i);
      ProtoApiAddress addr2 = h2List.get(i);
      if (addr1.getTs() > addr2.getTs())
        list.add(addr1);
      else
        list.add(addr2);
    }
    return ProtoCrHash.newBuilder()
      .setTs(Math.max(hash1.getTs(), hash2.getTs()))
      .setCls(hash1.getCls())
      .setNumSegment(count)
      .addAllSegmentAddr(list)
      .build();
  }


  public void initLocal(EmbeddedCacheManager cacheManager) {
    if (localAdvertiseAddress==null) {
      var registry = InternalCacheManager.of(cacheManager);
      var transport = registry.getComponent(Transport.class);
      JGroupsAddress address = (JGroupsAddress) transport.getPhysicalAddresses().getFirst();
      IpAddress ipAddress = (IpAddress) address.getJGroupsAddress();
      localAdvertiseAddress = ipAddress.getIpAddress().getHostAddress();
    }
  }

  public String getLocalAdvertiseAddress() {
    return localAdvertiseAddress;
  }
}
