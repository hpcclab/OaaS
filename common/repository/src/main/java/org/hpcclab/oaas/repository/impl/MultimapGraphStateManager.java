package org.hpcclab.oaas.repository.impl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.repository.function.AbstractGraphStateManager;
import org.infinispan.client.hotrod.multimap.RemoteMultimapCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

//@ApplicationScoped
@Deprecated(forRemoval = true)
public class MultimapGraphStateManager extends AbstractGraphStateManager {

  RemoteMultimapCache<String, String> edgeMap;

  public MultimapGraphStateManager() {
    super();
  }

//  @Inject
  public MultimapGraphStateManager(OaasObjectRepository objRepo,
                                   RemoteMultimapCache<String, String> edgeMap) {
    super(objRepo);
    this.edgeMap = edgeMap;
  }


  @Override
  public Uni<Collection<String>> getAllEdge(String srcId) {
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(edgeMap.get(srcId));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

//  @Override
//  public Uni<Boolean> containEdge(String srcId, String desId) {
//    var ctx = Vertx.currentContext();
//    var uni = Uni.createFrom().completionStage(
//      edgeMap.containsEntry(srcId,desId));
//    if (ctx!=null)
//      uni = uni.emitOn(ctx::runOnContext);
//    return uni;
//  }

  @Override
  public Uni<Void> persistEdge(String srcId, String desId) {
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(
      edgeMap.put(srcId,desId));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> list) {
    var ctx = Vertx.currentContext();
    var uni = Multi.createFrom().iterable(list)
      .onItem().transformToUniAndMerge( entry -> persistEdge(entry.getKey(), entry.getValue()))
      .collect().last();
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }
}
