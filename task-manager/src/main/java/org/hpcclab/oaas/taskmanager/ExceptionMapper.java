package org.hpcclab.oaas.taskmanager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.vertx.core.json.JsonObject;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExceptionMapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

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

  @ServerExceptionMapper(JsonParseException.class)
  public Response exceptionMapper(JsonParseException jsonParseException) {
    return Response.status(400)
      .entity(new JsonObject()
        .put("msg", jsonParseException.getMessage()))
      .build();
  }

  @ServerExceptionMapper(ConstraintViolationException.class)
  public Response exceptionMapper(ConstraintViolationException exception) {
    return Response.status(400)
      .entity(new JsonObject()
        .put("msg", "Message body is not valid")
        .put("violations", exception.getConstraintViolations()
          .stream()
          .map(cv -> cv.getPropertyPath().toString() + " " + cv.getMessage())
          .collect(Collectors.toList())
        ))
      .build();
  }
}
