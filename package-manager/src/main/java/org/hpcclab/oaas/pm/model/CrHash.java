package org.hpcclab.oaas.pm.model;

import com.arangodb.serde.jackson.Key;
import org.eclipse.collections.api.factory.Lists;

import java.util.List;

public record CrHash(String cls, int numSegment, List<ApiAddress> segmentAddr, long ts){
  @Key
  public String getKey() {
    return cls;
  }
  public record ApiAddress(String host, int port, long ts) {}

  public static CrHash merge(CrHash h1, CrHash h2) {
    var newer = h1.ts > h2.ts ? h1 : h2;
    var older = h1.ts > h2.ts ? h2 : h1;
    var addrList = Lists.mutable.ofAll(newer.segmentAddr);
    for (int i = 0; i < h2.segmentAddr().size(); i++) {
      var newAddr = newer.segmentAddr.get(i);
      var oldAddr = older.segmentAddr.get(i);
      if (oldAddr.ts > newAddr.ts) {
        addrList.set(i, oldAddr);
      }
//      else {
//        addrList.set(i, newAddr);
//      }
    }
    return new CrHash(newer.cls, addrList.size(), addrList, newer.ts);
  }
}
