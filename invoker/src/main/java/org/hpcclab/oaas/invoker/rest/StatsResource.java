package org.hpcclab.oaas.invoker.rest;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("stats")
public class StatsResource {
  @Inject
  EmbeddedIspnObjectRepository objectRepository;

  @GET
  @Path("ispn")
  @RunOnVirtualThread
  public JsonObject ispn() {
      var objStats = objectRepository.getCache().getStats();
      return JsonObject.of("obj", JsonObject.mapFrom(objStats));
  }
}
