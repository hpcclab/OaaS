package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;

import java.util.List;

public interface DataUrlAllocator {
  Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests);
}
