package org.hpcclab.oaas.sa.adapter;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class HttpDataRelayer implements DataRelayer {
  private static final Logger logger = LoggerFactory.getLogger( HttpDataRelayer.class );
  final WebClient webClient;

  @Inject
  public HttpDataRelayer(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  @Override
  public Uni<Response> relay(String url) {
    return webClient.getAbs(url)
      .send()
      .map(resp -> {
        if (resp.statusCode()==200) {
          var buffer = resp.bodyAsBuffer();
          logger.debug("Relaying data from '{}' with {} bytes",
            url, buffer==null ? 0:buffer.length());
          return Response.ok(buffer).build();
        } else {
          logger.warn("Error relaying data from '{}' code {}",
            url, resp.statusCode());
          return Response.status(Response.Status.BAD_GATEWAY)
            .build();
        }
      });
  }
}
