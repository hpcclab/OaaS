package org.hpcclab.oaas.invoker.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invoker.metrics.RequestCounterMap;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.JsonBytes;
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
public class ClassInvocationResource {
  final HashAwareInvocationHandler hashAwareInvocationHandler;
  final InvocationReqHandler invocationReqHandler;
  final ObjectRepoManager objectRepoManager;
  final RequestCounterMap requestCounterMap;

  public ClassInvocationResource(HashAwareInvocationHandler hashAwareInvocationHandler,
                                 InvocationReqHandler invocationReqHandler,
                                 ObjectRepoManager objectRepoManager,
                                 RequestCounterMap requestCounterMap) {
    this.hashAwareInvocationHandler = hashAwareInvocationHandler;
    this.invocationReqHandler = invocationReqHandler;
    this.objectRepoManager = objectRepoManager;
    this.requestCounterMap = requestCounterMap;
  }

  @GET
  @Path("invokes/{fb}")
  public Uni<InvocationResponse> invoke(String cls,
                                        String fb,
                                        @QueryParam("_async") @DefaultValue("false") boolean async,
                                        @Context UriInfo uriInfo) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    DSMap args = DSMap.mutable();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      if (!entry.getKey().startsWith("_"))
        args.put(entry.getKey(), entry.getValue().getFirst());
    }
    InvocationRequest oal = InvocationRequest.builder()
      .cls(cls)
      .fb(fb)
      .args(args)
      .build();
    requestCounterMap.increase(cls, fb);
    if (async) {
      return invocationReqHandler.enqueue(oal);
    }
    return hashAwareInvocationHandler.invoke(oal);
  }

  @POST
  @Path("invokes/{fb}")
  public Uni<InvocationResponse> invokeWithBody(String cls,
                                                String fb,
                                                @BeanParam InvokeParameters params,
                                                @Context UriInfo uriInfo,
                                                ObjectNode body) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    DSMap args = DSMap.mutable();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      if (!entry.getKey().startsWith("_"))
        args.put(entry.getKey(), entry.getValue().getFirst());
    }
    InvocationRequest oal = InvocationRequest.builder()
      .cls(cls)
      .fb(fb)
      .args(args)
      .body(new JsonBytes(body))
      .build();
    requestCounterMap.increase(cls, fb);
    if (params.async) {
      return invocationReqHandler.enqueue(oal)
        .map(params::filter);
    }
    return hashAwareInvocationHandler.invoke(oal)
      .map(params::filter);
  }

}
