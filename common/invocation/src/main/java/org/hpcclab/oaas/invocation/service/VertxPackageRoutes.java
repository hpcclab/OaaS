package org.hpcclab.oaas.invocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
public class VertxPackageRoutes implements VertxRouteService {
  private static final Logger logger = LoggerFactory.getLogger(VertxPackageRoutes.class);
  final ClassRepository classRepo;
  final FunctionRepository funcRepo;
  final PackageValidator validator;
  final ClassResolver classResolver;
  final ProtoMapper protoMapper;
  final PackageDeployer packageDeployer;
  final ObjectMapper yamlMapper;

  public VertxPackageRoutes(ClassRepository classRepo,
                            FunctionRepository funcRepo,
                            PackageValidator validator,
                            ClassResolver classResolver,
                            ProtoMapper protoMapper,
                            PackageDeployer packageDeployer) {
    this.classRepo = classRepo;
    this.funcRepo = funcRepo;
    this.validator = validator;
    this.classResolver = classResolver;
    this.protoMapper = protoMapper;
    this.packageDeployer = packageDeployer;
    yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  @Override
  public void mountRouter(Router router) {
    router.route().failureHandler(this::handleException);
    router.post("/packages")
      .consumes(APP_JSON)
      .produces(APP_JSON)
      .blockingHandler(this::handlePkg);
    router.post("/packages")
      .consumes("text/x-yaml")
      .produces(APP_JSON)
      .blockingHandler(this::handlePkgYaml);
    router.get("/classes")
      .produces(APP_JSON)
      .respond(this::listCls);
    router.get("/classes/:clsKey")
      .produces(APP_JSON)
      .respond(this::getCls);
    router.delete("/classes/:clsKey")
      .produces(APP_JSON)
      .blockingHandler(ctx -> ctx.jsonAndAwait(deleteCls(ctx)));
    router.get("/functions")
      .produces(APP_JSON)
      .respond(this::listFn);
    router.get("/functions/:fnKey")
      .produces(APP_JSON)
      .respond(this::getFn);
    router.delete("/functions/:fnKey")
      .produces(APP_JSON)
      .blockingHandler(ctx -> ctx.jsonAndAwait(deleteFn(ctx)));
  }

  private void handlePkgYaml(RoutingContext ctx) {
    try {
      var pkg = yamlMapper.readValue(ctx.body().buffer().getBytes(), OPackage.class);
      var out = createPackage(pkg);
      ctx.jsonAndAwait(out);
    } catch (IOException e) {
      ctx.fail(e);
    }
  }

  private void handlePkg(RoutingContext ctx) {
    var pkg = ctx.body().asPojo(OPackage.class);
    var out = createPackage(pkg);
    ctx.jsonAndAwait(out);
  }


  public OPackage createPackage(OPackage packageContainer) {
    var pkg = validator.validate(packageContainer);
    var classes = pkg.getClasses();
    var functions = pkg.getFunctions();
    var clsMap = classes.stream()
      .collect(Collectors.toMap(OClass::getKey, Function.identity()));
    var changedClasses = classResolver.resolveInheritance(clsMap);
    var pkgCls = changedClasses.values()
      .stream()
      .filter(cls -> Objects.equals(cls.getPkg(), pkg.getName()))
      .toList();
    pkg.setClasses(pkgCls);

    refresh(pkg.getClasses());
    refreshFn(pkg.getFunctions());

    packageDeployer.deploy(pkg);

    var partitioned = changedClasses.values()
      .stream()
      .collect(Collectors.partitioningBy(cls -> cls.getRev()==null));
    var newClasses = partitioned.get(true);
    var oldClasses = partitioned.get(false);
    classRepo
      .atomic().persistWithRevAsync(oldClasses)
      .await().indefinitely();
    classRepo.persist(newClasses);
    funcRepo.persist(functions);

    if (logger.isDebugEnabled())
      logger.debug("pkg {}", Json.encodePrettily(pkg));

    return pkg;
  }


  private void refresh(Collection<OClass> clsList) {
    var keys = clsList.stream().map(OClass::getKey).toList();
    var clsMap = classRepo.list(keys);
    for (OClass cls : clsList) {
      var oldCls = clsMap.get(cls.getKey());
      if (oldCls==null)
        continue;
      cls.setStatus(oldCls.getStatus());
      logger.debug("refresh cls {}", cls.getKey());
    }
  }

  private void refreshFn(Collection<OFunction> fnList) {
    var keys = fnList.stream().map(OFunction::getKey).toList();
    var fnMap = funcRepo.list(keys);
    for (var fn : fnList) {
      var oldFn = fnMap.get(fn.getKey());
      if (oldFn==null)
        continue;
      fn.setStatus(oldFn.getStatus());
      logger.debug("refresh fn {} {}", fn.getKey(), fn.getStatus());
    }
  }

  Uni<Pagination<OClass>> listCls(RoutingContext ctx) {
    var offset = getQueryAsLong(ctx, "offset", 0);
    var limit = getQueryAsLong(ctx, "limit", 20);
    var sort = getQueryAsStr(ctx, "sort", "_key");
    var desc = getQueryAsBool(ctx, "desc", false);
    if (classRepo instanceof MapEntityRepository<?, ?> map) {
      Collection values = map.getMap().values();
      int size = map.getMap().size();
      return Uni.createFrom()
        .item(new Pagination<OClass>(size, 0, size, List.copyOf(values)));
    } else {
      return classRepo.getQueryService()
        .sortedPaginationAsync(sort, desc, offset, (int) limit);
    }
  }
  Uni<Pagination<OFunction>> listFn(RoutingContext ctx) {
    var offset = getQueryAsLong(ctx, "offset", 0);
    var limit = getQueryAsLong(ctx, "limit", 20);
    var sort = getQueryAsStr(ctx, "sort", "_key");
    var desc = getQueryAsBool(ctx, "desc", false);
    if (funcRepo instanceof MapEntityRepository<?, ?> map) {
      Collection values = map.getMap().values();
      int size = map.getMap().size();
      return Uni.createFrom()
        .item(new Pagination<OFunction>(size, 0, size, List.copyOf(values)));
    } else {
      return funcRepo.getQueryService()
        .sortedPaginationAsync(sort, desc, offset, (int) limit);
    }
  }

  Uni<OClass> getCls(RoutingContext ctx) {
    String clsKey = ctx.pathParam("clsKey");
    return classRepo.async().getAsync(clsKey)
      .onItem().ifNull()
      .failWith(() -> StdOaasException.notFoundCls(clsKey, 404));
  }

  public Uni<OFunction> getFn(RoutingContext ctx) {
    String funcKey = ctx.pathParam("fnKey");
    return funcRepo.async().getAsync(funcKey)
      .onItem().ifNull()
      .failWith(() -> StdOaasException.notFoundFunc(funcKey, 404));
  }

  OClass deleteCls(RoutingContext ctx) {
    String clsKey = ctx.pathParam("clsKey");
    var cls = classRepo.get(clsKey);
    if (cls==null) {
      ctx.fail(StdOaasException.notFoundCls(clsKey, 404));
      return null;
    }
    if (cls.getStatus()!=null && cls.getStatus().getCrId() > 0) {
      packageDeployer.detach(cls);
    }
    classRepo.remove(clsKey);
    return cls;
  }

  OFunction deleteFn(RoutingContext ctx) {
    String funcKey = ctx.pathParam("fnKey");
    OFunction removed = funcRepo.remove(funcKey);
    if (removed == null)
      ctx.fail(StdOaasException.notFoundFunc(funcKey, 404));
    packageDeployer.detach(removed);
    return removed;
  }
}
