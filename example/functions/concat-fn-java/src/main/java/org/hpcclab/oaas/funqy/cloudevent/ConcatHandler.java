package org.hpcclab.oaas.funqy.cloudevent;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.object.OOUpdate;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@ApplicationScoped
public class ConcatHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConcatHandler.class);
  @Inject
  Vertx vertx;
  WebClient webClient;
  @Inject
  MeterRegistry meterRegistry;

  @PostConstruct
  void setup() {
    webClient = WebClient.create(vertx);
  }

  @Funq()
  @CloudEventMapping(
    trigger = "oaas.task",
    responseSource = "oaas/concat",
    responseType = "oaas.task.result"
  )
  public Uni<CloudEvent<TaskCompletion>> handle(OTask task, @Context CloudEvent<OTask> event) {
    var args = task.getArgs();
    var inputUrl = task.getMainKeys().get("text");
    var inPlace = Boolean.parseBoolean(args.getOrDefault("INPLACE", "false"));
    var update = new OOUpdate().setUpdatedKeys(Set.of("text"));
    var completionBuilder = TaskCompletion.builder()
      .id(task.getId())
      .success(true);
    if (inPlace)
      completionBuilder.main(update);
    else
      completionBuilder.output(update);
    var allocUrl = inPlace ? task.getAllocMainUrl():task.getAllocOutputUrl();
    if (allocUrl==null)
      return Uni.createFrom()
        .item(CloudEventBuilder.create()
          .id(task.getId())
          .type("oaas.task.result")
          .source("oaas/concat")
          .build(completionBuilder.success(false).errorMsg("Can not find proper alloc URL").build()));

    return getText(inputUrl)
      .flatMap(text -> {
        var append = args.getOrDefault("APPEND", "");
        text += append;
        return putText(
          allocUrl,
          text);
      })
      .map(text -> CloudEventBuilder.create()
        .id(task.getId())
        .type("oaas.task.result")
        .source("oaas/concat")
        .build(completionBuilder.build())
      );
  }

  Uni<String> alloc(String url) {
    return webClient.getAbs(url)
      .expect(ResponsePredicate.SC_OK)
      .send()
      .map(HttpResponse::bodyAsJsonObject)
      .map(js -> js.getString("text"));
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

  Uni<String> putText(String allocUrl, String text) {
    var timer = Timer.builder("putText")
      .publishPercentiles(0.5, 0.75, 0.9, 0.95)
      .register(meterRegistry);
    var sample = Timer.start(meterRegistry);
    return alloc(allocUrl)
      .flatMap(putUrl -> webClient.putAbs(putUrl)
        .expect(ResponsePredicate.SC_SUCCESS)
        .sendBuffer(Buffer.buffer(text))
        .invoke(() -> sample.stop(timer))
        .replaceWith(text)
      );
  }
}
