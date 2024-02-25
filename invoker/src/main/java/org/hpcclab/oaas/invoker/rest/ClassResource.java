package org.hpcclab.oaas.invoker.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.repository.ObjectRepoManager;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@Path("/api/classes/{cls}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClassResource {
  final InvokerManager invokerManager;
  final HashAwareInvocationHandler hashAwareInvocationHandler;
  final ObjectRepoManager objectRepoManager;

  public ClassResource(InvokerManager invokerManager, HashAwareInvocationHandler hashAwareInvocationHandler, ObjectRepoManager objectRepoManager) {
    this.invokerManager = invokerManager;
    this.hashAwareInvocationHandler = hashAwareInvocationHandler;
    this.objectRepoManager = objectRepoManager;
  }

  @GET
  @Path("invokes/{fb}")
  public Uni<InvocationResponse> invoke(String cls,
                                        String fb,
                                        @Context UriInfo uriInfo) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    DSMap args = DSMap.mutable();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      if (!entry.getKey().startsWith("_"))
        args.put(entry.getKey(), entry.getValue().getFirst());
    }
    List<String> inputs = queryParameters.getOrDefault("_inputs", List.of());
    return hashAwareInvocationHandler.invoke(ObjectAccessLanguage.builder()
      .cls(cls)
      .fb(fb)
      .args(args)
      .inputs(inputs)
      .build());
  }

  @POST
  @Path("invokes/{fb}")
  public Uni<InvocationResponse> invokeWithBody(String cls,
                                                String fb,
                                                @Context UriInfo uriInfo,
                                                ObjectNode body) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    DSMap args = DSMap.mutable();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      if (!entry.getKey().startsWith("_"))
        args.put(entry.getKey(), entry.getValue().getFirst());
    }
    List<String> inputs = queryParameters.getOrDefault("_inputs", List.of());
    return hashAwareInvocationHandler.invoke(ObjectAccessLanguage.builder()
      .cls(cls)
      .fb(fb)
      .args(args)
      .inputs(inputs)
      .body(body)
      .build());
  }
}
