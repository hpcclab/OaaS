package org.hpcclab.msc.object;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.vertx.core.json.JsonObject;
import org.hpcclab.msc.object.exception.NoStackException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class ExceptionMapper {
  @ServerExceptionMapper(IllegalArgumentException.class)
  public Response exceptionMapper(IllegalArgumentException illegalArgumentException) {
    return Response.status(404)
      .entity(new JsonObject()
        .put("msg", illegalArgumentException.getMessage()))
      .build();
  }

  @ServerExceptionMapper(WebApplicationException.class)
  public Response exceptionMapper(WebApplicationException webApplicationException) {
    return Response.fromResponse(webApplicationException.getResponse())
      .entity(new JsonObject()
        .put("msg", webApplicationException.getMessage()))
      .build();
  }

  @ServerExceptionMapper(NoStackException.class)
  public Response exceptionMapper(NoStackException noStackException) {
    return Response.status(noStackException.getCode())
      .entity(new JsonObject()
        .put("msg", noStackException.getMessage()))
      .build();
  }
  @ServerExceptionMapper(JsonMappingException.class)
  public Response exceptionMapper(JsonMappingException jsonMappingException) {
    return Response.status(400)
      .entity(new JsonObject()
        .put("msg", jsonMappingException.getMessage()))
      .build();
  }
}
