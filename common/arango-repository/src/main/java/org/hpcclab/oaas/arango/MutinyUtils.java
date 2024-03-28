package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCursorAsync;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.mutiny.core.Vertx;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class MutinyUtils {
  public static  <T> Uni<T> createUni(Supplier<CompletionStage<T>> stage) {
    var uni = Uni.createFrom().completionStage(stage);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public static <T> Multi<T> toMulti(Supplier<CompletionStage<ArangoCursorAsync<T>>> supplier) {
    return Multi.createFrom().emitter(emitter -> pipe(supplier.get(), emitter));
  }

  public static <T> void pipe(CompletionStage<ArangoCursorAsync<T>> stage, MultiEmitter<? super T> emitter) {
    stage.whenComplete((cursorAsync, throwable) -> {
      if (throwable!=null) emitter.fail(throwable);
      for (T t : cursorAsync.getResult()) {
        emitter.emit(t);
      }
      if (Boolean.TRUE.equals(cursorAsync.hasMore())) {
        pipe(cursorAsync.nextBatch(), emitter);
      } else {
        cursorAsync.close();
        emitter.complete();
      }
    });
  }

}
