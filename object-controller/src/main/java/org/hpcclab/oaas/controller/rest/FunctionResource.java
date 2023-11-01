package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.controller.service.ProvisionPublisher;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public class FunctionResource {
  @Inject
  FunctionRepository funcRepo;
  @Inject
  PackageResource packageResource;
  @Inject
  ProvisionPublisher provisionPublisher;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());


  @GET
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasFunction>> list(@RestQuery Long offset,
                                            @RestQuery Integer limit,
                                            @RestQuery String sort,
                                            @RestQuery @DefaultValue("false") boolean desc) {
    if (offset==null) offset = 0L;
    if (limit==null) limit = 20;
    if (sort==null) sort = "_key";
    return funcRepo.getQueryService()
      .sortedPaginationAsync(sort, desc, offset, limit);
  }

  @GET
  @Path("{funcKey}")
  @JsonView(Views.Public.class)
  public Uni<OaasFunction> get(String funcKey) {
    return funcRepo.async().getAsync(funcKey)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @DELETE
  @Path("{funcKey}")
  @JsonView(Views.Public.class)
  public Uni<OaasFunction> delete(String funcKey) {
    return funcRepo.async().removeAsync(funcKey)
      .onItem().ifNull().failWith(NotFoundException::new)
      .call(__ -> provisionPublisher.submitDeleteFn(funcKey));
  }
}
