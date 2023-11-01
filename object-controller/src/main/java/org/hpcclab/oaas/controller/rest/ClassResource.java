package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.controller.service.ProvisionPublisher;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
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
  ProvisionPublisher provisionPublisher;

  @GET
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasClass>> list(@RestQuery Long offset,
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
  public Uni<OaasClass> get(String clsKey) {
    return classRepo.async().getAsync(clsKey)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @DELETE
  @Path("{clsKey}")
  @JsonView(Views.Public.class)
  public Uni<OaasClass> delete(String clsKey) {
    return classRepo.async().removeAsync(clsKey)
      .onItem().ifNull().failWith(NotFoundException::new)
      .call(__ -> provisionPublisher.submitDeleteCls(clsKey));
  }
}
