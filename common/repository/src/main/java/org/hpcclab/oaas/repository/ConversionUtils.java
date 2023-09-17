package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

import java.util.concurrent.CompletionStage;

public class ConversionUtils {
  private ConversionUtils(){}
  public  static <V> Uni<V> toUni(CompletionStage<V> stage) {
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(stage);
    if (ctx!=null)
      uni = uni
        .emitOn(ctx::runOnContext);
    return uni;
  }
}
