package org.hpcclab.oaas.invocation.service;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.hpcclab.oaas.model.exception.StdOaasException;

/**
 * @author Pawissanutt
 */
public interface VertxRouteService {
  void mountRouter(Router router);

  default void handleException(RoutingContext ctx) {
    Throwable failure = ctx.failure();
    if (failure instanceof StdOaasException std) {
      ctx.response().setStatusCode(std.getCode());
      ctx.jsonAndForget(JsonObject.of("msg", std.getMessage()));

    }
    ctx.next();
  }


  default long getQueryAsLong(RoutingContext ctx, String key, long defaultVal) {
    String str = ctx.queryParams().get(key);
    if (str == null) return defaultVal;
    return Long.parseLong(str);
  }
  default String getQueryAsStr(RoutingContext ctx, String key, String defaultVal) {
    String str = ctx.queryParams().get(key);
    if (str == null) return defaultVal;
    return str;
  }
  default boolean getQueryAsBool(RoutingContext ctx, String key, boolean defaultVal) {
    String str = ctx.queryParams().get(key);
    if (str == null) return defaultVal;
    return Boolean.parseBoolean(str);
  }


  String APP_JSON = "application/json";
}
