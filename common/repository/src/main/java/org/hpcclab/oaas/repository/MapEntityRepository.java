package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.object.GOObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapEntityRepository<K, V extends HasKey<K>> implements EntityRepository<K, V>
  , AsyncEntityRepository<K, V> {
  MutableMap<K, V> map;
  Function<V, K> keyExtractor;

  public MapEntityRepository(MutableMap<K, V> map,
                             Function<V, K> keyExtractor) {
    this.map = map;
    this.keyExtractor = keyExtractor;
  }

  @Override
  public V get(K key) {
    var v = map.get(key);
    if (v instanceof Copyable)
      return ((Copyable<V>) v).copy();
    return v;
  }

  @Override
  public AsyncEntityRepository<K, V> async() {
    return this;
  }

  @Override
  public Uni<V> getAsync(K key) {
    return Uni.createFrom().item(get(key));
  }

  @Override
  public Map<K, V> list(Collection<K> keys) {
    return map.select((k, v) -> keys.contains(k));
  }

  @Override
  public Uni<Map<K, V>> listAsync(Collection<K> keys) {
    return Uni.createFrom().item(list(keys));
  }

  @Override
  public Uni<List<V>> orderedListAsync(Collection<K> keys) {
    return AsyncEntityRepository.super.orderedListAsync(keys);
  }


  @Override
  public V remove(K key) {
    return map.remove(key);
  }

  @Override
  public Uni<V> removeAsync(K key) {
    return Uni.createFrom().item(map.remove(key));
  }

  @Override
  public Uni<Void> deleteAsync(K key) {
    return AsyncEntityRepository.super.deleteAsync(key);
  }

  @Override
  public V put(K key, V value) {
    if (value instanceof Copyable<?>)
      return map.put(key, ((Copyable<V>) value).copy());
    return map.put(key, value);
  }

  @Override
  public Uni<V> putAsync(K key, V value) {
    return Uni.createFrom().item(put(key, value));
  }

  @Override
  public V persist(V v) {
    return put(v.getKey(), v);
  }

  @Override
  public Uni<V> persistAsync(V v) {
    return Uni.createFrom().item(persist(v));
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection) {
    var map = collection.stream().collect(Collectors.toMap(HasKey::getKey, Function.identity()));
    return putAllAsync(map);
  }


  //  @Override
  public Uni<Void> putAllAsync(Map<K, V> m) {
    m.forEach(this::put);
    return Uni.createFrom().voidItem();
  }

  @Override
  public V compute(K key, BiFunction<K, V, V> function) {
    return map.compute(key, (k, v) -> {
      var out = function.apply(k, v);
      if (out instanceof Copyable<?>)
        return ((Copyable<V>) out).copy();
      return out;
    });
  }

  @Override
  public Uni<V> computeAsync(K key, BiFunction<K, V, V> function) {

    return Uni.createFrom().item(compute(key, function));
  }

  public MutableMap<K, V> getMap() {
    return map;
  }

  @Override
  public AtomicOperationService<K, V> atomic() {
    return new DefaultAtomicOperationService<>(this);
  }

  @Override
  public QueryService<K, V> getQueryService() {
    throw new UnsupportedOperationException();
  }

  public static class MapObjectRepository extends MapEntityRepository<String, GOObject> implements ObjectRepository {
    public MapObjectRepository(MutableMap<String, GOObject> map) {
      super(map, GOObject::getKey);
    }
  }

  public static class MapClsRepository extends MapEntityRepository<String, OClass> implements ClassRepository {
    public MapClsRepository(MutableMap<String, OClass> map) {
      super(map, OClass::getKey);
    }

    @Override
    public List<OClass> listSubCls(String clsKey) {
      return map.values()
        .stream()
        .filter(cls -> cls.getResolved() != null &&
          cls.getResolved().getIdentities() != null &&
          cls.getResolved().getIdentities().contains(clsKey))
        .toList();
    }
  }

  public static class MapFnRepository extends MapEntityRepository<String, OFunction> implements FunctionRepository {
    public MapFnRepository(MutableMap<String, OFunction> map) {
      super(map, OFunction::getKey);
    }
  }

  public static class MapObjectRepoManager extends ObjectRepoManager {

    Function<String, OClass> clsLoader;

    public MapObjectRepoManager(MutableMap<String, GOObject> map,
                                Function<String, OClass> clsLoader
    ) {
      var bagMultimap = map.groupBy(o -> o.getMeta().getCls());
      bagMultimap.keyMultiValuePairsView()
        .forEach(pair -> {
          MutableMap<String, GOObject> objs = pair.getTwo()
            .toMap(GOObject::getKey, o -> o);
          repoMap.put(pair.getOne(), new MapObjectRepository(objs));
        });
      this.clsLoader = clsLoader;
    }

    @Override
    public ObjectRepository createRepo(OClass cls) {
      return new MapObjectRepository(Maps.mutable.empty());
    }

    @Override
    protected OClass load(String clsKey) {
      return clsLoader.apply(clsKey);
    }
  }
}
