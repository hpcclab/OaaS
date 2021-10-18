package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.model.OaasClassDto;
import org.hpcclab.msc.object.model.OaasFunctionDto;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/batch")
public interface BatchService {

  @POST
  Uni<Batch> create(Batch batch);

  @POST
  @Consumes("text/x-yaml")
  Uni<Batch> createByYaml(String body);

  public static class Batch{
    List<OaasClassDto> classes = List.of();
    List<OaasFunctionDto> functions = List.of();
  }
}
