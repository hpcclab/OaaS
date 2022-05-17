package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.EntityRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MockGraphStateManager extends AbstractGraphStateManager {

  MutableMultimap<String, String> multimap = Multimaps.mutable.set.empty();

  MutableMap<String, OaasObject> objectMap;

  public MockGraphStateManager(EntityRepository<String, OaasObject> objectRepo,
                               MutableMap<String, OaasObject> objectMap) {
    super(objectRepo);
    this.objectMap = objectMap;
  }

  @Override
  public Uni<Collection<String>> getAllEdge(String srcId) {
    return Uni.createFrom().item(multimap.get(srcId));
  }

  @Override
  public Uni<Boolean> containEdge(String srcId, String desId) {
    return Uni.createFrom().item(multimap.containsKeyAndValue(srcId, desId));
  }

  @Override
  public Uni<Boolean> persistEdge(String srcId, String desId) {
    return Uni.createFrom().item(multimap.put(srcId, desId));
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap) {
    var edges = Lists.fixedSize.ofAll(edgeMap)
      .collect(Tuples::pairFrom);
    return Uni.createFrom().item(multimap.putAllPairs(edges))
      .replaceWithVoid();
  }
}
