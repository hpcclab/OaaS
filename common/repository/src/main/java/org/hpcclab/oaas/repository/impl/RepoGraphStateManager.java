package org.hpcclab.oaas.repository.impl;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.function.AbstractGraphStateManager;
import org.infinispan.client.hotrod.multimap.RemoteMultimapCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RepoGraphStateManager extends AbstractGraphStateManager {

  RemoteMultimapCache<String, String> edgeMap;

  public RepoGraphStateManager() {
    super();
  }

  @Inject
  public RepoGraphStateManager(OaasObjectRepository objRepo,
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

  @Override
  public Uni<Boolean> containEdge(String srcId, String desId) {
    return null;
  }

  @Override
  public Uni<Boolean> persistEdge(String srcId, String desId) {
    return null;
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap) {
    return null;
  }
}
