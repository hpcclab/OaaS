package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.controller.OcConfig;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.controller.service.FunctionProvisionPublisher;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.jboss.resteasy.reactive.RestQuery;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
  OcConfig config;
  @Inject
  FunctionProvisionPublisher provisionPublisher;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());


  @GET
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasFunction>> list(@RestQuery Long offset,
                                            @RestQuery Integer limit) {
    if (offset== null) offset = 0L;
    if (limit== null) limit = 20;
    return funcRepo.sortedPaginationAsync("name", false, offset, limit);
  }

  @POST
  @JsonView(Views.Public.class)
  public Uni<List<OaasFunction>> create(@RestQuery boolean update,
                                        List<OaasFunction> functionDtos) {
    var uni = Multi.createFrom().iterable(functionDtos)
      .onItem()
      .transformToUniAndConcatenate(funcDto -> funcRepo.persistAsync(funcDto)
      )
      .collect().asList();
    if (config.kafkaEnabled()) {
      return uni
        .call(functions -> provisionPublisher.submitNewFunction(functions.stream()));
    } else {
      return uni;
    }
  }

  @POST
  @Consumes("text/x-yaml")
  @JsonView(Views.Public.class)
  public Uni<List<OaasFunction>> createByYaml(@RestQuery boolean update,
                                              String body) {
    try {
      var funcs = yamlMapper.readValue(body, OaasFunction[].class);
      return create(update, Arrays.asList(funcs));
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  @GET
  @Path("{funcName}")
  @JsonView(Views.Public.class)
  public Uni<OaasFunction> get(String funcName) {
    return funcRepo.getAsync(funcName)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
