package org.hpcclab.oaas.funqy.cloudevent;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.hpcclab.oaas.model.object.OOUpdate;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;

import java.util.Set;

@ApplicationScoped
@Path("/")
public class ConcatHandler {
  final Vertx vertx;
  final WebClient webClient;
  final MeterRegistry meterRegistry;

  @Inject
  public ConcatHandler(Vertx vertx, MeterRegistry meterRegistry) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    webClient = WebClient.create(vertx);
  }

  @POST
  public Uni<OTaskCompletion> handle(OTask task) {
    var args = task.getArgs();
    var inputUrl = task.getMainGetKeys().get("text");
    var inPlace = Boolean.parseBoolean(args.getOrDefault("INPLACE", "false"));
    var update = new OOUpdate().setUpdatedKeys(Set.of("text"));
    var completionBuilder = OTaskCompletion.builder()
      .id(task.getId())
      .success(true);
    if (inPlace)
      completionBuilder.main(update);
    else
      completionBuilder.output(update);
    var putUrl = inPlace ? task.getMainPutKeys().get("text"):task.getOutputKeys().get("text");
    if (putUrl==null)
      return Uni.createFrom()
        .item(completionBuilder.success(false)
          .errorMsg("Can not find proper alloc URL")
          .build()
        );

    return getText(inputUrl)
      .flatMap(text -> {
        var append = args.getOrDefault("APPEND", "");
        text += append;
        return putText(
          putUrl,
          text);
      })
      .map(text -> completionBuilder.build());
  }


  Uni<String> getText(String url) {
    var timer = Timer.builder("getText")
      .publishPercentiles(0.5, 0.75, 0.9, 0.95)
      .register(meterRegistry);
    var sample = Timer.start(meterRegistry);
    return webClient.getAbs(url)
      .expect(ResponsePredicate.SC_OK)
      .send()
      .map(HttpResponse::bodyAsString)
      .invoke(() -> sample.stop(timer));
  }

  Uni<String> putText(String putUrl, String text) {
    var timer = Timer.builder("putText")
      .publishPercentiles(0.5, 0.75, 0.9, 0.95)
      .register(meterRegistry);
    var sample = Timer.start(meterRegistry);
    return webClient.putAbs(putUrl)
        .expect(ResponsePredicate.SC_SUCCESS)
        .sendBuffer(Buffer.buffer(text))
        .invoke(() -> sample.stop(timer))
        .replaceWith(text);
  }
}
