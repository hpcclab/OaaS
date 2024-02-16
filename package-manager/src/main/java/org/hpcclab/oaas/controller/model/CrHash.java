package org.hpcclab.oaas.controller.model;

import com.arangodb.serde.jackson.Key;
import org.eclipse.collections.api.factory.Lists;

import java.util.List;

public record CrHash(String cls, int numSegment, List<ApiAddress> segmentAddr){
  @Key
  public String getKey() {
    return cls;
  }
  public record ApiAddress(String host, int port) {}

  public static CrHash merge(CrHash h1, CrHash h2) {
    var addrList = Lists.mutable.ofAll(h1.segmentAddr);
    for (int i = 0; i < h2.segmentAddr().size(); i++) {
      var addr = h2.segmentAddr.get(i);
      if (addr == null || addr.port < 0) continue;
      if (i < addrList.size()) {
        addrList.set(i, addr);
      } else {
        addrList.add(addr);
      }
    }
    return new CrHash(h1.cls,addrList.size(), addrList);
  }
}
