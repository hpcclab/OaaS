package org.hpcclab.oaas.invoker.lookup;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.hpcclab.oaas.proto.PaginateQuery;
import org.hpcclab.oaas.proto.ProtoApiAddress;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
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
import java.util.concurrent.CompletionStage;

/**
 * @author Pawissanutt
 */
public class HashRegistry {
  private static final Logger logger = LoggerFactory.getLogger(HashRegistry.class);
  private static final ProtoApiAddress NULL = ProtoApiAddress.newBuilder().setPort(-1).build();
  final InternalCrStateService crStateService;
  final Cache<String, ProtoCrHash> cacheMap;
  String localAdvertiseAddress;

  public HashRegistry(InternalCrStateService crStateService, Cache<String, ProtoCrHash> cacheMap) {
    this.crStateService = crStateService;
    this.cacheMap = cacheMap;
  }

  static ProtoCrHash merge(ProtoCrHash hash1, ProtoCrHash hash2) {
    var newer = hash1.getTs() > hash2.getTs() ? hash1:hash2;
    var older = hash1.getTs() > hash2.getTs() ? hash2:hash1;
    var count = newer.getNumSegment();
    List<ProtoApiAddress> list = new ArrayList<>(newer.getSegmentAddrList());
    for (int i = 0; i < count; i++) {
      if (older.getSegmentAddrCount() <= i) continue;
      ProtoApiAddress newerAddr = newer.getSegmentAddr(i);
      ProtoApiAddress olderAddr = older.getSegmentAddr(i);
      if (olderAddr.getTs() > newerAddr.getTs())
        list.set(i, olderAddr);
    }
    return ProtoCrHash.newBuilder()
      .setTs(Math.max(hash1.getTs(), hash2.getTs()))
      .setCls(hash1.getCls())
      .setNumSegment(count)
      .addAllSegmentAddr(list)
      .build();
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
        var old = i < current.getSegmentAddrCount() ? current.getSegmentAddr(i):NULL;
        builder.addSegmentAddr(old);
      }
    }
    ProtoCrHash protoCrHash = builder
      .setTs(System.currentTimeMillis())
      .build();
    return crStateService.updateHash(protoCrHash)
      .call(h -> Uni.createFrom().completionStage(cacheMap.putAsync(cls, protoCrHash)));
  }

  public Uni<Void> warmCache() {
    if (!cacheMap.isEmpty()) return Uni.createFrom().voidItem(); // no need to warm
    return crStateService.listHash(PaginateQuery.newBuilder().setLimit(100000).setOffset(0).build())
      .call(crHash -> Uni.createFrom().completionStage(updateManaged(crHash)))
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

  public CompletionStage<ProtoCrHash> updateManaged(ProtoCrHash protoCrHash) {
    logger.info("update local hash registry '{}'", protoCrHash.getCls());
    return cacheMap.computeAsync(protoCrHash.getCls(),
      (k, v) -> {
        if (v==null) return protoCrHash;
        return merge(v, protoCrHash);
      });
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
