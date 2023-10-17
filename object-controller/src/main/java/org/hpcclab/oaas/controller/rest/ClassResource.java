package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.controller.mapper.CtxMapper;
import org.hpcclab.oaas.controller.service.ProvisionPublisher;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
  PackageResource packageResource;

  @Inject
  ProvisionPublisher provisionPublisher;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

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
  @Path("{name}/objects")
  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasObject>> listObject(String name,
                                                @RestQuery String sort,
                                                @RestQuery @DefaultValue("false") boolean desc,
                                                @RestQuery Long offset,
                                                @RestQuery Integer limit,
                                                @RestQuery @DefaultValue("false") boolean includeSub) {
    final var fOffset = offset==null ? 0L:offset;
    final var fLimit = limit==null ? 20:limit;
    final var fSort = sort==null ? "_key":sort;
    var uni = includeSub ?
      classRepo.listSubClsKeys(name)
      :Uni.createFrom().item(List.of(name));
    Uni<Pagination<OaasObject>> uni2;
    if (fSort.equals("_"))
      uni2 = uni.flatMap(keys -> objectRepo.listByCls(keys, fOffset, fLimit));
    else
      uni2 = uni.flatMap(keys -> objectRepo.sortedListByCls(keys, fSort, desc, fOffset, fLimit));
    return uni2;
  }

  @POST
  @JsonView(Views.Public.class)
  @Deprecated(forRemoval = true)
  @RunOnVirtualThread
  public OaasClass create(@RestQuery boolean update, OaasClass cls) {
    cls.validate();
    var pkgName = cls.getPkg()==null ? "default":cls.getPkg();
    var pkg = new OaasPackageContainer();
    pkg.setClasses(List.of(cls))
      .setName(pkgName);
    var module = packageResource.create(update, false, pkg);
    return module.getClasses().isEmpty() ? null:module.getClasses().get(0);
  }

  @PATCH
  @Path("{name}")
  @JsonView(Views.Public.class)
  public Uni<OaasClass> patch(String name, OaasClass clsPatch) {
    return classRepo.async().getAsync(name)
      .onItem().ifNull().failWith(NotFoundException::new)
      .flatMap(cls -> {
        oaasMapper.set(clsPatch, cls);
        cls.validate();
        return classRepo.async().persistAsync(cls);
      });
  }

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  @Deprecated(forRemoval = true)
  @JsonView(Views.Public.class)
  @RunOnVirtualThread
  public OaasClass createByYaml(@RestQuery boolean update, String body) {
    try {
      var cls = yamlMapper.readValue(body, OaasClass.class);
      return create(update, cls);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
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
