package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.AbstractMutableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *  Dual-String Map (Map with string as key and value) that wrap on eclipse {@link MutableMap}
 */
public class DSMap extends AbstractMutableMap<String, String> {
  private static final DSMap EMPTY_MAP = new DSMap(Maps.fixedSize.of());

  MutableMap<String, String> wrapped;


  public DSMap() {
    this.wrapped = Maps.mutable.empty();
  }

  public DSMap(MutableMap<String, String> wrapped) {
    this.wrapped = wrapped;
  }

  @JsonCreator
  public DSMap(Map<String, String> wrapped) {
    this.wrapped = Maps.mutable.ofMap(wrapped);
  }

  public static DSMap of() {
    return EMPTY_MAP;
  }

  public static DSMap mutable(){
    return new DSMap(Maps.mutable.empty());
  }

  public static DSMap of(String k1, String v1) {
    return wrap(Maps.fixedSize.of(k1, v1));
  }

  public static DSMap of(String k1, String v1,
                         String k2, String v2) {
    return wrap(Maps.fixedSize.of(k1, v1, k2, v2));
  }

  public static DSMap of(String k1, String v1,
                         String k2, String v2,
                         String k3, String v3) {
    return wrap(Maps.fixedSize.of(k1, v1,
      k2, v2, k3, v3));
  }

  public static DSMap of(String... kv) {
    if (kv.length % 2!=0)
      throw new IllegalArgumentException();
    MutableMap<String, String> m = Maps.mutable.ofInitialCapacity(kv.length / 2);
    for (int i = 0; i < kv.length; i += 2) {
      m.put(kv[i], kv[i + 1]);
    }
    return wrap(m);
  }

  public static DSMap copy(Map<String, String> map) {
    if (map == null) return of();
    return wrap(Maps.mutable.ofMap(map));
  }

  public static DSMap wrap(MutableMap<String, String> map) {
    return new DSMap(map);
  }


  @Override
  public <E> MutableMap<String, String> collectKeysAndValues(Iterable<E> iterable, Function<? super E, ? extends String> keyFunction, Function<? super E, ? extends String> valueFunction) {
    return wrapped.collectKeysAndValues(iterable, keyFunction, valueFunction);
  }

  @Override
  public String removeKey(String key) {
    return wrapped.removeKey(key);
  }

  @Override
  public MutableMap<String, String> newEmpty() {
    return wrapped.newEmpty();
  }

  @Override
  public MutableMap<String, String> clone() {
    return wrapped.clone();
  }

  public DSMap copy() {
    return wrap(clone());
  }

  @Override
  public <K, V> MutableMap<K, V> newEmpty(int capacity) {
    return (MutableMap<K, V>) wrapped.newEmpty();
  }

  @JsonValue
  public MutableMap<String, String> unwrap() {
    return wrapped;
  }


  @Override
  public String put(String key, String value) {
    return wrapped.put(key, value);
  }

  @Override
  public String remove(Object key) {
    return wrapped.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m) {
    wrapped.putAll(m);
  }

  @Override
  public void clear() {
    wrapped.clear();
  }

  @Override
  public Set<String> keySet() {
    return wrapped.keySet();
  }

  @Override
  public Collection<String> values() {
    return wrapped.values();
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return wrapped.entrySet();
  }

  @Override
  public String get(Object key) {
    return wrapped.get(key);
  }

  @Override
  public boolean containsKey(Object key) {
    return wrapped.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return wrapped.containsValue(value);
  }

  @Override
  public void forEachKeyValue(Procedure2<? super String, ? super String> procedure) {
    wrapped.forEachKeyValue(procedure);
  }

  @Override
  public int size() {
    return wrapped.size();
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }
}
