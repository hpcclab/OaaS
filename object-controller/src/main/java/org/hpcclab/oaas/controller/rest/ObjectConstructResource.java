package org.hpcclab.oaas.controller.rest;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.controller.service.DataAllocationService;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectConstruction;
import org.hpcclab.oaas.model.object.ObjectConstructionResponse;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/object-construct")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ObjectConstructResource {

  @Inject
  OaasClassRepository clsRepo;
  @Inject
  OaasObjectRepository objRepo;
  @Inject
  DataAllocationService allocationService;


  @POST
  public Uni<ObjectConstructionResponse> construct(ObjectConstruction construction) {
    var cls = clsRepo.get(construction.getCls());
    if (cls == null) throw NoStackException.notFoundCls400(construction.getCls());
    var obj = OaasObject.createFromClasses(cls);
    obj.setId(UUID.randomUUID());
    obj.setEmbeddedRecord(construction.getEmbeddedRecord());
    obj.setLabels(construction.getLabels());
    obj.setOrigin(new OaasObjectOrigin().setRootId(obj.getId()));
    obj.getState().setOverrideUrls(construction.getOverrideUrls());
    DataAllocateRequest request = new DataAllocateRequest();
    request.setOid(obj.getId().toString())
      .setPublicUrl(true)
      .setKeys(Map.of("s3", List.copyOf(construction.getKeys())));
    return allocationService.allocate(request)
      .flatMap(response -> objRepo.persistAsync(obj)
          .map(ignore -> new ObjectConstructionResponse(obj,response.getUrlKeys()))
      );
  }
}
