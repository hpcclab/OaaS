package org.hpcclab.oaas.invocation.service;

import io.vertx.mutiny.ext.web.RoutingContext;
import org.hpcclab.oaas.model.invocation.InvocationResponse;

/**
 * @author Pawissanutt
 */
public record InvokeParameters(
  boolean async,
  boolean showMain,
  boolean showStat,
  boolean showOutput,
  boolean showAll
) {
  public static InvokeParameters create(RoutingContext context) {

    return new InvokeParameters(
      getBool(context, "_async", false),
      getBool(context, "_showMain", false),
      getBool(context, "_showStat", false),
      getBool(context, "_showOutput", true),
      getBool(context, "_showAll", false)
    );
  }


  public InvocationResponse filter(InvocationResponse response) {
    if (showAll) return response;
    var builder = response.toBuilder();
    if (!showMain)
      builder.main(null);
    if (!showStat)
      builder.stats(null);
    if (!showOutput)
      builder.output(null);
    return builder
      .build();
  }


  public static boolean getBool(RoutingContext context,
                                String name,
                                boolean defaultBool) {
    return context.queryParam(name)
      .stream()
      .findAny()
      .map(s -> !s.equals("false"))
      .orElse(defaultBool);
  }
}
