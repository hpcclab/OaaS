package org.hpcclab.oaas.invoker.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.storage.S3Adapter;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class S3DataUrlAllocator implements DataUrlAllocator {

  S3Adapter s3Adapter;

  @Inject
  public S3DataUrlAllocator(S3Adapter s3Adapter) {
    this.s3Adapter = s3Adapter;
  }

  @Override
  public Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests) {
    return Multi.createFrom().iterable(requests)
      .map(req -> {
        Map<String, String> map = s3Adapter.allocate(req);
        return new DataAllocateResponse(req.getOid(), map);
      })
      .collect().asList();
  }
}
