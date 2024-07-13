package org.hpcclab.oprc.cli.service;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.service.VertxInvocationService;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class DevServerService {
  private static final Logger logger = LoggerFactory.getLogger( DevServerService.class );
  @Inject
  Vertx vertx;
  @Inject
  VertxInvocationService vertxInvocationService;
  @Inject
  LocalDevManager localDevManager;
  AtomicBoolean running = new AtomicBoolean(false);

  public void start(int port) throws IOException {
    localDevManager.init();
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port));
    Router router = Router.router(vertx);
    Router subRouter = Router.router(vertx);
    vertxInvocationService.mountRouter(subRouter);
    router.route().handler(BodyHandler.create());
    router.route("/api/*")
      .subRouter(subRouter);
    httpServer.requestHandler(router)
      .listenAndAwait();
    running.set(true);
  }

  public void stop() {
    if (running.get()) {
      try {
        logger.debug("persisting objects to local files...");
        localDevManager.persistObject();
        logger.debug("done persisting objects to local files");
      } catch (IOException e) {
        throw new StdOaasException(e);
      }
    }
  }


  public void onShutdown(@Observes ShutdownEvent event) {
    stop();
  }
}
