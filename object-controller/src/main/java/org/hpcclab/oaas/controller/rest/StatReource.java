package org.hpcclab.oaas.controller.rest;

import org.hpcclab.oaas.repository.impl.OaasClassRepository;
import org.hpcclab.oaas.repository.impl.OaasFuncRepository;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
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
  OaasClassRepository clsRepo;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;

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
