package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  ObjectRepository objectRepo;

  @JsonView(Views.Public.class)
  public Uni<Pagination<OaasObject>> list(Integer offset, Integer limit) {
    if (offset==null) offset = 0;
    if (limit==null) limit = 20;
    if (limit > 100) limit = 100;
    return objectRepo.sortedPaginationAsync("_key",offset, limit);
  }


  @JsonView(Views.Public.class)
  public Uni<OaasObject> get(String id) {
    return objectRepo.getAsync(id)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @JsonView(Views.Public.class)
  public Uni<OaasObject> delete(String id) {
    return objectRepo.removeAsync(id);
  }
}
