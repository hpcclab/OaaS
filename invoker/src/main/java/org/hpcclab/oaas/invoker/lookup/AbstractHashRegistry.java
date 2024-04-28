package org.hpcclab.oaas.invoker.lookup;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.hpcclab.oaas.proto.PaginateQuery;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.IntSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hpcclab.oaas.model.cr.CrHash.NULL;

/**
 * @author Pawissanutt
 */
public abstract class AbstractHashRegistry implements HashRegistry {
  private static final Logger logger = LoggerFactory.getLogger(AbstractHashRegistry.class);
  final InternalCrStateService crStateService;
  final ProtoMapper protoMapper = new ProtoMapperImpl();
  String localAdvertiseAddress;

  protected AbstractHashRegistry(InternalCrStateService crStateService) {
    this.crStateService = crStateService;
  }

  public Uni<ProtoCrHash> push(String cls, int totalSegment, IntSet segments, CrHash.ApiAddress address) {
    var current = get(cls);
    if (current==null) current = new CrHash(cls, totalSegment, List.of(), 0);
    var builder = CrHash.builder()
      .cls(cls)
      .numSegment(totalSegment);
    List<CrHash.ApiAddress> newAddr = new ArrayList<>(totalSegment);
    var currentAddr = current.segmentAddr();
    for (int i = 0; i < totalSegment; i++) {
      if (segments.contains(i)) {
        newAddr.add(address);
      } else {
        CrHash.ApiAddress old = i < currentAddr.size() ? currentAddr.get(i):NULL;
        newAddr.add(old);
      }
    }
    CrHash crHash = builder
      .segmentAddr(newAddr)
      .ts(System.currentTimeMillis())
      .build();
    return crStateService.updateHash(protoMapper.toProto(crHash))
      .invoke(h -> store(crHash));
  }

  public Uni<Void> warmCache() {
    if (!isEmpty()) return Uni.createFrom().voidItem();
    logger.info("warm HashRegistry cache");// no need to warm
    return crStateService.listHash(PaginateQuery.newBuilder().setLimit(100000).setOffset(0).build())
      .invoke(this::storeExternal)
      .collect().last().replaceWithVoid();
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

  public void storeExternal(ProtoCrHash protoCrHash) {
    logger.info("update local hash registry '{}'", protoCrHash.getCls());
    var crHash = protoMapper.fromProto(protoCrHash);
    storeMerge(crHash);
  }

  @Override
  public String getLocalAdvertiseAddress() {
    return localAdvertiseAddress;
  }

  @Override
  public void setLocalAdvertiseAddress(String localAdvertiseAddress) {
    this.localAdvertiseAddress = localAdvertiseAddress;
  }

  abstract void store(CrHash crHash);

  abstract void storeMerge(CrHash crHash);

  abstract CrHash get(String cls);

  abstract boolean isEmpty();
}
