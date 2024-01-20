package org.hpcclab.oaas.controller.rest;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.controller.model.Orbit;
import org.hpcclab.oaas.controller.service.OrbitStateManager;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.proto.*;
import org.jboss.resteasy.reactive.RestMulti;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/api/orbits")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrbitResource {
  OrbitStateManager stateManager;
  OrbitManagerGrpc.OrbitManagerBlockingStub orbitManager;

  @Inject
  public OrbitResource(OrbitStateManager stateManager,
                       @GrpcClient("orbit-manager") OrbitManagerGrpc.OrbitManagerBlockingStub orbitManager) {
    this.stateManager = stateManager;
    this.orbitManager = orbitManager;
  }

  @GET
  public Uni<Pagination<Orbit>> listOrbit(@RestQuery Integer limit,
                                          @RestQuery Integer offset) {
    return stateManager.getRepo()
      .getQueryService()
      .paginationAsync(offset==null ? 0:offset, limit==null ? 20:limit);
  }

  @Path("{id}")
  @DELETE
  @RunOnVirtualThread
  public OprcResponse delete(String id) {
    var orbit = stateManager.get(id).await().indefinitely();
    if (orbit != null) {
      var res =  orbitManager.destroy(orbit);
      stateManager.getRepo().delete(String.valueOf(orbit.getId()));
      return res;
    }
    throw new NotFoundException();
  }
}
