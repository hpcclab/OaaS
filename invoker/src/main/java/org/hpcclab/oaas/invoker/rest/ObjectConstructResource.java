package org.hpcclab.oaas.invoker.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.invoker.service.DataAllocationService;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.object.ObjectConstructResponse;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.ObjectRepository;

import java.util.List;
import java.util.Map;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/object-construct")
public class ObjectConstructResource {

  @Inject
  ClassRepository clsRepo;
  @Inject
  ObjectRepository objRepo;
  @Inject
  DataAllocationService allocationService;
  @Inject
  OaasObjectFactory objectFactory;


  @POST
  @JsonView(Views.Public.class)
  public Uni<ObjectConstructResponse> construct(ObjectConstructRequest construction) {
    var cls = clsRepo.get(construction.getCls());
    if (cls==null) throw NoStackException.notFoundCls400(construction.getCls());
    return switch (cls.getObjectType()) {
      case SIMPLE, COMPOUND -> constructSimple(construction, cls);
    };
  }

  private void linkReference(ObjectConstructRequest request,
                             OaasObject obj,
                             OaasClass cls) {
    //TODO validate the references of request
    obj.setRefs(request.getRefs());
  }

  private Uni<ObjectConstructResponse> constructSimple(ObjectConstructRequest construction,
                                                       OaasClass cls) {
    var obj = objectFactory.createBase(construction, cls);
    linkReference(construction, obj, cls);
    var stateSpec = cls.getStateSpec();
    if (stateSpec==null) return objRepo.persistAsync(obj)
      .map(ignore -> new ObjectConstructResponse(obj, Map.of()));

    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> construction.getKeys().contains(k.getName()));
    if (ks.isEmpty()) {
      return objRepo.persistAsync(obj)
        .map(ignored -> new ObjectConstructResponse(obj, Map.of()));
    }
    DataAllocateRequest request = new DataAllocateRequest(obj.getId(), ks, cls.getStateSpec().getDefaultProvider(), true);
    return allocationService.allocate(List.of(request))
      .map(list -> new ObjectConstructResponse(obj, list.get(0).getUrlKeys()))
      .call(() -> objRepo.persistAsync(obj));
  }
}
