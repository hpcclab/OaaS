package org.hpcclab.oaas.test;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MockDataUrlAllocator implements DataUrlAllocator {
  @Override
  public Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests) {
    var resList = Lists.fixedSize.ofAll(requests)
      .collect(req -> {
        var m = req.getKeys().stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
        return new DataAllocateResponse(req.getOid(), m);
      });
    return Uni.createFrom().item(resList);
  }
}
