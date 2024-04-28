package org.hpcclab.oaas.invoker.lookup;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.infinispan.AdvancedCache;

/**
 * @author Pawissanutt
 */
public interface HashRegistry {
  Uni<Void> warmCache();
  CrHash.ApiAddress get(String cls, int segment);
  void storeManaged(ProtoCrHash protoCrHash);
  Uni<Void> updateManaged(String cls, AdvancedCache<?, ?> cache, int port);
  void setLocalAdvertiseAddress(String localAdvertiseAddress);
  String getLocalAdvertiseAddress();
}
