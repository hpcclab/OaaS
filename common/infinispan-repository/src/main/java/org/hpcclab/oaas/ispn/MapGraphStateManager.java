package org.hpcclab.oaas.ispn;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.object.ObjectInvNode;
import org.hpcclab.oaas.repository.AbstractGraphStateManager;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.client.hotrod.RemoteCache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class MapGraphStateManager extends AbstractGraphStateManager {

  RemoteCache<String, ObjectInvNode> graphMap;

  public MapGraphStateManager() {
    super();
  }

  @Inject
  public MapGraphStateManager(ObjectRepository objRepo,
                              @Remote(InfinispanInit.INVOCATION_GRAPH_CACHE)
                              RemoteCache<String, ObjectInvNode> graphMap) {
    super(objRepo);
    this.graphMap = graphMap;
  }


  @Override
  public Uni<? extends Collection<String>> getAllEdge(String srcId) {
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(graphMap.getAsync(srcId));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni
      .map(og -> {
        if (og == null) return Set.of();
        return og.getNextInv();
      });
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
      graphMap.computeAsync(srcId, (key, og)-> {
        if (og == null) og = new ObjectInvNode();
        og.getNextInv().add(desId);
        return og;
      }));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni
      .replaceWithVoid();
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
