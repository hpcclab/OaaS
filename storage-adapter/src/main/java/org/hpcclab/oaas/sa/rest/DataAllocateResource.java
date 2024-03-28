package org.hpcclab.oaas.sa.rest;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.storage.S3Adapter;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import java.util.List;
import java.util.Map;

@Path("/allocate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataAllocateResource {
  final S3Adapter s3Adapter;
  final ClassRepository clsRepo;

  @Inject
  public DataAllocateResource(S3Adapter s3Adapter, ClassRepository clsRepo) {
    this.s3Adapter = s3Adapter;
    this.clsRepo = clsRepo;
  }

  @POST
  public Multi<DataAllocateResponse> allocate(List<DataAllocateRequest> requests) {
    return Multi.createFrom().iterable(requests)
      .map(req -> {
        Map<String, String> map = s3Adapter.allocate(req);
        return new DataAllocateResponse(req.getOid(), map);
      });
  }


  @POST
  @Path("{oid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> allocate(@RestPath String oid,
                                      @RestQuery String contextKey,
                                      List<String> keys) {
    var dac = DataAccessContext.parse(contextKey);
    var req = new DataAllocateRequest(oid, dac.getVid(), keys, "s3", false);
    return s3Adapter.allocate(req);
  }

  @GET
  @Path("{oid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getAllocatedUrls(String oid,
                                              @RestQuery String contextKey) {
    var dac = DataAccessContext.parse(contextKey);
    var clsName = dac.getCls();
    var cls = clsRepo.get(clsName);
    if (cls==null) throw StdOaasException.notFoundCls400(clsName);
    var req = new DataAllocateRequest(oid,
      dac.getVid(),
      cls.getStateSpec().getKeySpecs().stream().map(KeySpecification::getName).toList(),
      null,
      false);
    return s3Adapter.allocate(req);
  }
}
