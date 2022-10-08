package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.controller.mapper.CtxMapper;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/classes")
public class ClassResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassResource.class);
  @Inject
  ClassRepository classRepo;
  @Inject
  ObjectRepository objectRepo;
  @Inject
  CtxMapper oaasMapper;
  @Inject
  ModuleResource moduleResource;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @GET
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasClass>> list(@RestQuery Long offset,
                                         @RestQuery Integer limit) {
    if (offset==null) offset = 0L;
    if (limit==null) limit = 20;
    return classRepo.sortedPaginationAsync("name", offset, limit);
  }

  @GET
  @Path("{name}/objects")
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasObject>> listObject(String name,
                                                @RestQuery Long offset,
                                                @RestQuery Integer limit) {
    if (offset==null) offset = 0L;
    if (limit==null) limit = 20;
    return objectRepo.listByCls(name, offset, limit);
  }

  @POST
  @JsonView(Views.Public.class)
  public Uni<OaasClass> create(@RestQuery boolean update, OaasClass cls) {
    cls.validate();
    return moduleResource.create(update, new ModuleService.Module()
      .setClasses(List.of(cls)))
      .map(module ->module.getClasses().isEmpty()? null:module.getClasses()
        .get(0)
      );
  }

  @PATCH
  @Path("{name}")
  @JsonView(Views.Public.class)
  public Uni<OaasClass> patch(String name, OaasClass clsPatch) {
    return classRepo.getAsync(name)
      .onItem().ifNull().failWith(NotFoundException::new)
      .flatMap(cls -> {
        oaasMapper.set(clsPatch, cls);
        cls.validate();
        return classRepo.persistAsync(cls);
      });
  }

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  @JsonView(Views.Public.class)
  public Uni<OaasClass> createByYaml(@RestQuery boolean update, String body) {
    try {
      var cls = yamlMapper.readValue(body, OaasClass.class);
      return create(update, cls);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  @GET
  @Path("{name}")
  @JsonView(Views.Public.class)
  public Uni<OaasClass> get(String name) {
    return classRepo.getAsync(name)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @DELETE
  @Path("{name}")
  @JsonView(Views.Public.class)
  public Uni<OaasClass> delete(String name) {
    return classRepo.removeAsync(name)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
