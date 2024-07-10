package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExceptionMapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

  @ServerExceptionMapper(StatusRuntimeException.class)
  public Response exceptionMapper(StatusRuntimeException statusRuntimeException) {
    Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

    if (statusRuntimeException.getStatus().getCode()==Code.NOT_FOUND)
      status = Response.Status.NOT_FOUND;
    else if (statusRuntimeException.getStatus().getCode()==Code.UNAVAILABLE)
      status = Response.Status.SERVICE_UNAVAILABLE;
    else if (statusRuntimeException.getStatus().getCode()==Code.RESOURCE_EXHAUSTED)
      status = Response.Status.TOO_MANY_REQUESTS;
    else if (statusRuntimeException.getStatus().getCode()==Code.INVALID_ARGUMENT)
      status = Response.Status.BAD_REQUEST;
    else  if (statusRuntimeException.getStatus().getCode()==Code.UNIMPLEMENTED)
      status = Response.Status.NOT_IMPLEMENTED;
    else if (statusRuntimeException.getStatus().getCode()==Code.UNAUTHENTICATED)
      status = Response.Status.UNAUTHORIZED;

    if (LOGGER.isWarnEnabled() && status==Response.Status.INTERNAL_SERVER_ERROR) {
      LOGGER.warn("mapping StatusRuntimeException: {}", statusRuntimeException.getMessage());
    } else if (LOGGER.isDebugEnabled())
      LOGGER.debug("mapping StatusRuntimeException({})", status);
    return Response.status(status)
      .entity(new JsonObject()
        .put("msg", statusRuntimeException.getMessage()))
      .build();
  }

  @ServerExceptionMapper(IllegalArgumentException.class)
  public Response exceptionMapper(IllegalArgumentException illegalArgumentException) {
    return Response.status(404)
      .entity(new JsonObject()
        .put("msg", illegalArgumentException.getMessage()))
      .build();
  }

  @ServerExceptionMapper(StdOaasException.class)
  public Response exceptionMapper(StdOaasException exception) {
    if (exception.getCode()==500 && LOGGER.isWarnEnabled()) {
      LOGGER.warn("mapping exception({})", exception.getCode(), exception);
    } else if (LOGGER.isDebugEnabled())
      LOGGER.debug("mapping exception({})", exception.getCode(), exception);
    return Response.status(exception.getCode())
      .entity(new JsonObject()
        .put("msg", exception.getMessage()))
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
          .toList()
        ))
      .build();
  }
}
