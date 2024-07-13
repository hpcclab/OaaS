package org.hpcclab.oprc.cli.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.service.VertxInvocationService;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class ServerRouteProducer {
  @Produces
  @ApplicationScoped
  VertxInvocationService vertxInvocationService(LocationAwareInvocationForwarder invocationForwarder,
                                                InvocationReqHandler invocationReqHandler,
                                                ObjectMapper mapper) {
    return new VertxInvocationService(
      invocationForwarder,
      invocationReqHandler,
      mapper
    );
  }
}
