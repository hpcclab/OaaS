package org.hpcclab.oprc.cli.service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.LoggerHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.service.VertxInvocationRoutes;
import org.hpcclab.oaas.invocation.service.VertxPackageRoutes;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pawissanutt
 */
@RegisterForReflection(
  targets = {Pagination.class}
)
@ApplicationScoped
public class DevServerService {
  private static final Logger logger = LoggerFactory.getLogger( DevServerService.class );
  @Inject
  Vertx vertx;
  @Inject
  VertxInvocationRoutes vertxInvocationRoutes;
  @Inject
  VertxPackageRoutes vertxPackageRoutes;
  @Inject
  LocalDevManager localDevManager;
  AtomicBoolean running = new AtomicBoolean(false);

  public void start(int port) throws IOException {
    localDevManager.init();
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port));
    Router router = Router.router(vertx);
    Router invocationSubRouter = Router.router(vertx);
    vertxInvocationRoutes.mountRouter(invocationSubRouter);
    Router packageSubRouter = Router.router(vertx);
    vertxPackageRoutes.mountRouter(packageSubRouter);
    router.route().handler(BodyHandler.create())
      .handler(LoggerHandler.create())
      ;
    router.route("/api/*")
      .subRouter(invocationSubRouter);
    router.route("/api/*")
      .subRouter(packageSubRouter);
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
