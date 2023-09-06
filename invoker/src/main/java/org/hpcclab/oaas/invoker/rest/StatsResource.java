package org.hpcclab.oaas.invoker.rest;

import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnObjectRepository;

@Path("stats")
public class StatsResource {

  @Inject
  EmbeddedIspnObjectRepository objectRepository;

  @GET
  @Path("ispn")
  public JsonObject ispn() {
      var objStats = objectRepository.getCache().getStats();
      return JsonObject.of("obj", JsonObject.mapFrom(objStats));
  }
}
