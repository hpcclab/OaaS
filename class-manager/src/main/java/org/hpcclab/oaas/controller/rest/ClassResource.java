package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.controller.service.OrbitStateManager;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ClassRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/classes")
public class ClassResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassResource.class);
  @Inject
  ClassRepository classRepo;
  @Inject
  OrbitStateManager orbitStateManager;

  @GET
  @JsonView(Views.Public.class)
  public Uni<Pagination<OClass>> list(@RestQuery Long offset,
                                      @RestQuery Integer limit,
                                      @RestQuery String sort,
                                      @RestQuery @DefaultValue("false") boolean desc) {
    if (offset==null) offset = 0L;
    if (limit==null) limit = 20;
    if (sort==null) sort = "_key";
    return classRepo.getQueryService()
      .sortedPaginationAsync(sort, desc, offset, limit);
  }


  @GET
  @Path("{clsKey}")
  @JsonView(Views.Public.class)
  public Uni<OClass> get(String clsKey) {
    return classRepo.async().getAsync(clsKey)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @DELETE
  @Path("{clsKey}")
  @JsonView(Views.Public.class)
  @RunOnVirtualThread
  public OClass delete(String clsKey) {
    var cls = classRepo.get(clsKey);
    if (cls==null) {
      throw new NotFoundException();
    }
    if (cls.getStatus()!=null && cls.getStatus().getOrbitId() > 0) {
      orbitStateManager.detach(cls);
    }
    classRepo.remove(clsKey);
    return cls;
  }
}
