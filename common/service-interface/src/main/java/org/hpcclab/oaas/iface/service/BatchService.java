package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;

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

  @Data
  @Accessors(chain = true)
  public static class Batch{
    List<OaasClass> classes = List.of();
    List<OaasFunction> functions = List.of();
  }
}
