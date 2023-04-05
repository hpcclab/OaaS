package org.hpcclab.oaas.arango;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class ConversionUtils {
  public static  <T> Uni<T> createUni(Supplier<CompletionStage<T>> stage) {
    var uni = Uni.createFrom().completionStage(stage);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }
}
