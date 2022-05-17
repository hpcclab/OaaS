package org.hpcclab.oaas.controller.rest;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.iface.service.ObjectService;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  OaasObjectRepository objectRepo;

  public Uni<Pagination<OaasObject>> list(Integer offset, Integer limit) {
    if (offset==null) offset = 0;
    if (limit==null) limit = 20;
    if (limit > 100) limit = 100;
    var list = objectRepo.pagination(offset, limit);
    return Uni.createFrom().item(list);
  }

  public Uni<OaasObject> create(OaasObject creating) {
    return objectRepo.createRootAndPersist(creating)
      .onFailure().invoke(e -> LOGGER.error("error", e));
  }

  public Uni<OaasObject> get(String id) {
    return objectRepo.getAsync(id)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
