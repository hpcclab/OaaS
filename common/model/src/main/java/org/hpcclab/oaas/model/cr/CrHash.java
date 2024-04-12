package org.hpcclab.oaas.model.cr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.eclipse.collections.api.factory.Lists;
import org.infinispan.protostream.annotations.Proto;

import java.util.List;

@Proto
@Builder(toBuilder = true)
public record CrHash(String cls, int numSegment, List<ApiAddress> segmentAddr, long ts){
  @JsonProperty("_key")
  public String getKey() {
    return cls;
  }

  @Proto
  @Builder(toBuilder = true)
  public record ApiAddress(String host, int port, long ts) {}

  public static CrHash merge(CrHash h1, CrHash h2) {
    var newer = h1.ts > h2.ts ? h1 : h2;
    var older = h1.ts > h2.ts ? h2 : h1;
    var addrList = Lists.mutable.ofAll(newer.segmentAddr);
    for (int i = 0; i < h2.segmentAddr().size(); i++) {
      var newAddr = addrList.get(i);
      if (older.segmentAddr.size() <= i) continue;
      var oldAddr = older.segmentAddr.get(i);
      if (oldAddr.ts > newAddr.ts) {
        addrList.set(i, oldAddr);
      }
    }
    return new CrHash(newer.cls, addrList.size(), addrList, newer.ts);
  }

  public static ApiAddress NULL = new ApiAddress("", 0, 0);
}
