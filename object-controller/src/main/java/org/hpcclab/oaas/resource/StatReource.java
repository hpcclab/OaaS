package org.hpcclab.oaas.resource;

import org.eclipse.microprofile.openapi.models.servers.Server;
import org.hpcclab.oaas.repository.IfnpOaasClassRepository;
import org.hpcclab.oaas.repository.IfnpOaasFuncRepository;
import org.hpcclab.oaas.repository.IfnpOaasObjectRepository;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/stats")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StatReource {

  @Inject
  IfnpOaasClassRepository clsRepo;
  @Inject
  IfnpOaasObjectRepository objectRepo;
  @Inject
  IfnpOaasFuncRepository funcRepo;

  @GET
  @Path("class/client")
  public RemoteCacheClientStatisticsMXBean classClientStat() {
    return clsRepo.getRemoteCache().clientStatistics();
  }

  @GET
  @Path("class/server")
  public ServerStatistics classServerStat() {
    return clsRepo.getRemoteCache().serverStatistics();
  }

  @GET
  @Path("object/client")
  public RemoteCacheClientStatisticsMXBean objectClientStat() {
    return objectRepo.getRemoteCache().clientStatistics();
  }
  @GET
  @Path("object/server")
  public ServerStatistics objectServerStat() {
    return objectRepo.getRemoteCache().serverStatistics();
  }
  @GET
  @Path("function/client")
  public RemoteCacheClientStatisticsMXBean funcClientStat() {
    return funcRepo.getRemoteCache().clientStatistics();
  }
  @GET
  @Path("function/server")
  public ServerStatistics funcServerStat() {
    return funcRepo.getRemoteCache().serverStatistics();
  }
}
