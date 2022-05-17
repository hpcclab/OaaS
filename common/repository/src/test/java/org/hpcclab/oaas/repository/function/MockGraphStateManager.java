package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class MockGraphStateManager extends AbstractGraphStateManager {

  MutableMultimap<String, String> multimap = Multimaps.mutable.set.empty();

  MutableMap<String,OaasObject> objectMap;

  public MockGraphStateManager(MutableMap<String, OaasObject> objectMap) {
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
  @Override
  Uni<OaasObject> compute(String id, UnaryOperator<OaasObject> function) {
    var o = objectMap.compute(id, (k,v) -> function.apply(v));
    return Uni.createFrom().item(o);
  }

  @Override
  Uni<Void> persistAll(FunctionExecContext ctx) {
    objectMap.put(ctx.getOutput().getId(), ctx.getOutput());
    var m = Lists.fixedSize.ofAll(ctx.getSubOutputs())
      .groupByUniqueKey(OaasObject::getId);
    objectMap.putAll(m);
    return Uni.createFrom().voidItem();
  }

  @Override
  Uni<Collection<OaasObject>> getObjects(Collection<String> ids) {
    var c = objectMap.toList()
      .select(o -> ids.contains(o.getId()));
    return Uni.createFrom().item(c);
  }

  @Override
  Uni<Void> persist(OaasObject object) {
    objectMap.put(object.getId(), object);
    return Uni.createFrom().voidItem();
  }
}
