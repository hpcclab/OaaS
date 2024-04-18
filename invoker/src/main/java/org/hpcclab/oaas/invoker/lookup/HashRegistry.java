package org.hpcclab.oaas.invoker.lookup;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cr.CrHash;
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

import static org.hpcclab.oaas.model.cr.CrHash.NULL;

/**
 * @author Pawissanutt
 */
public class HashRegistry {
  private static final Logger logger = LoggerFactory.getLogger(HashRegistry.class);
  final InternalCrStateService crStateService;
  final Cache<String, CrHash> cacheMap;
  final ProtoMapper protoMapper = new ProtoMapperImpl();
  String localAdvertiseAddress;

  public HashRegistry(InternalCrStateService crStateService, Cache<String, CrHash> cacheMap) {
    this.crStateService = crStateService;
    this.cacheMap = cacheMap;
  }

  public Uni<ProtoCrHash> push(String cls, int totalSegment, IntSet segments, CrHash.ApiAddress address) {
    var current = cacheMap.get(cls);
    if (current == null) current = new CrHash(cls, totalSegment, List.of(), 0);
    var builder = CrHash.builder()
      .cls(cls)
      .numSegment(totalSegment);
    List<CrHash.ApiAddress> newAddr = new ArrayList<>(totalSegment);
    var currentAddr = current.segmentAddr();
    for (int i = 0; i < totalSegment; i++) {
      if (segments.contains(i)) {
        newAddr.add(address);
      } else {
        CrHash.ApiAddress old = i < currentAddr.size() ? currentAddr.get(i): NULL;
        newAddr.add(old);
      }
    }
    CrHash crHash = builder
      .segmentAddr(newAddr)
      .ts(System.currentTimeMillis())
      .build();
    return crStateService.updateHash(protoMapper.toProto(crHash))
      .call(h -> Uni.createFrom().completionStage(cacheMap.putAsync(cls, crHash)));
  }

  public Uni<Void> warmCache() {
    if (!cacheMap.isEmpty()) return Uni.createFrom().voidItem();
    logger.info("warm HashRegistry cache");// no need to warm
    return crStateService.listHash(PaginateQuery.newBuilder().setLimit(100000).setOffset(0).build())
      .call(crHash -> Uni.createFrom().completionStage(updateManaged(crHash)))
      .collect().last().replaceWithVoid();
  }

  public CrHash.ApiAddress get(String cls, int segment) {
    CrHash protoCrHash = cacheMap.get(cls);
    if (protoCrHash==null) return null;
    List<CrHash.ApiAddress> apiAddresses = protoCrHash.segmentAddr();
    if (apiAddresses.size() < segment) return null;
    return apiAddresses.get(segment);
  }

  public Uni<Void> updateManaged(String cls, AdvancedCache<?, ?> cache, int port) {
    var topology = cache.getAdvancedCache()
      .getDistributionManager()
      .getCacheTopology();
    return push(cls,
      topology.getNumSegments(),
      topology.getLocalPrimarySegments(),
      CrHash.ApiAddress.builder()
        .port(port)
        .ts(System.currentTimeMillis())
        .host(localAdvertiseAddress).build()
    ).replaceWithVoid();
  }

  public CompletionStage<CrHash> updateManaged(ProtoCrHash protoCrHash) {
    logger.info("update local hash registry '{}'", protoCrHash.getCls());
    var crHash = protoMapper.fromProto(protoCrHash);
    return cacheMap.computeAsync(crHash.cls(),
      (k, v) -> {
        if (v==null) return crHash;
        return CrHash.merge(v, crHash);
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
