package org.hpcclab.oaas.controller.rest;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.controller.service.DataAllocationService;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.object.ObjectConstructResponse;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/api/object-construct")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ObjectConstructResource {

  @Inject
  OaasClassRepository clsRepo;
  @Inject
  OaasObjectRepository objRepo;
  @Inject
  @RestClient
  DataAllocationService allocationService;
  @Inject
  OaasObjectFactory objectFactory;


  @POST
  public Uni<ObjectConstructResponse> construct(ObjectConstructRequest construction) {
    var cls = clsRepo.get(construction.getCls());
    if (cls==null) throw NoStackException.notFoundCls400(construction.getCls());
    return switch (cls.getObjectType()) {
      case SIMPLE, COMPOUND -> constructSimple(construction, cls);
      case STREAM -> constructStream(construction, cls);
    };
  }

  private Uni<ObjectConstructResponse> constructSimple(ObjectConstructRequest construction,
                                                       OaasClass cls) {
    var obj = objectFactory.createBase(construction, cls, objRepo.generateId());
    var stateSpec = cls.getStateSpec();
    if (stateSpec==null) return objRepo.persistAsync(obj)
      .map(ignore -> new ObjectConstructResponse(obj, Map.of()));

    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> construction.getKeys().contains(k.getName()));
    DataAllocateRequest request = new DataAllocateRequest(obj.getId(), ks, true);
    return allocationService.allocate(List.of(request))
      .map(list -> new ObjectConstructResponse(obj, list.get(0).getUrlKeys()))
      .call(() -> objRepo.persistAsync(obj));
  }

  private Uni<ObjectConstructResponse> constructStream(ObjectConstructRequest construction,
                                                       OaasClass cls) {
    var genericType = cls.getGenericType();
    var genericCls = clsRepo.get(genericType);
    var obj = objectFactory.createBase(construction, cls, objRepo.generateId());
    var sc = Lists.fixedSize.ofAll(construction.getStreamConstructs());
    var objStream = sc.collectWithIndex((c,i) -> objectFactory.createBase(c, genericCls, obj.getId() + '.' + i));
    var requestList = sc.zip(objStream).collect(pair -> {
      var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
        .select(k -> construction.getKeys().contains(k.getName()));
      return new DataAllocateRequest(pair.getTwo().getId(), ks, true);
    });

    return allocationService.allocate(requestList)
      .map(list -> aggregateResult(obj, objStream, list))
      .call(() -> {
        objStream.add(obj);
        return objRepo.persistAsync(objStream);
      });
  }

  private ObjectConstructResponse aggregateResult(OaasObject baseObj,
                                                  MutableList<OaasObject> objStream,
                                                  List<DataAllocateResponse> responses) {
    var respStream = objStream.zip(responses)
      .collect(pair -> new ObjectConstructResponse(pair.getOne(), pair.getTwo().getUrlKeys()));
    return new ObjectConstructResponse(baseObj, Map.of(), respStream);
  }

}
