package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public class ObjectResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  ObjectRepository objectRepo;

  @GET
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasObject>> list(@RestQuery Integer offset,
                                          @DefaultValue("false") @RestQuery boolean desc,
                                          @RestQuery Integer limit,
                                          @RestQuery String sort) {
    if (offset==null) offset = 0;
    if (limit==null) limit = 20;
    if (limit > 100) limit = 100;
    if (sort==null) sort = "_key";
    if (sort.equals("_"))
      return objectRepo.paginationAsync(offset, limit);
    return objectRepo.sortedPaginationAsync(sort, desc, offset, limit);
  }

  @GET
  @Path("{id}")
  @JsonView(Views.Public.class)
  public Uni<OaasObject> get(String id) {
    return objectRepo.getAsync(id)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @DELETE
  @Path("{id}")
  @JsonView(Views.Public.class)
  public Uni<OaasObject> delete(String id) {
    return objectRepo.removeAsync(id);
  }
}
