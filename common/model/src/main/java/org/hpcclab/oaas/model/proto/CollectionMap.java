package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.HasKey;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CollectionMap<V extends HasKey<String>>
        implements Collection<V> {
    private static final CollectionMap<? extends HasKey> EMPTY_MAP = new CollectionMap<>(Maps.fixedSize.of());

    MutableMap<String, V> wrapped;

    public CollectionMap() {
        this.wrapped = Maps.mutable.empty();
    }


    public CollectionMap(MutableMap<String, V> wrapped) {
        this.wrapped = wrapped;
    }

    @JsonCreator
    public CollectionMap(Map<String, V> wrapped) {
        this.wrapped = Maps.mutable.ofMap(wrapped);
    }

    public static <V extends HasKey<String>> CollectionMap<V> of() {
        return (CollectionMap<V>) EMPTY_MAP;
    }

    public static <V extends HasKey<String>> CollectionMap<V> ofMutable() {
        return wrap(Maps.mutable.empty());
    }

    public static <V extends HasKey<String>> CollectionMap<V> of(V value) {
        return new CollectionMap<>(Maps.fixedSize.of(value.getKey(), value));
    }

    public static <V extends HasKey<String>> CollectionMap<V> of(V v1, V v2) {
        return new CollectionMap<>(Maps.fixedSize.of(v1.getKey(), v1, v2.getKey(), v2));
    }

    public static <V extends HasKey<String>> CollectionMap<V> of(V v1, V v2, V v3) {
        return new CollectionMap<>(Maps.fixedSize.of(v1.getKey(), v1, v2.getKey(), v2, v3.getKey(), v3));
    }

    @SafeVarargs
    public static <V extends HasKey<String>> CollectionMap<V> of(V... v) {
        var m = Maps.fixedSize.<String, V>empty();
        for (V e : v) {
            m.put(e.getKey(), e);
        }
        return CollectionMap.wrap(m);
    }


    public static <V extends HasKey<String>> CollectionMap<V> wrap(MutableMap<String, V> map) {
        return new CollectionMap<>(map);
    }

    @JsonValue
    public MutableMap<String, V> getWrapped() {
        return wrapped;
    }


    public <E> MutableMap<String, V> collectKeysAndValues(Iterable<E> iterable, Function<? super E, ? extends String> keyFunction, Function<? super E, ? extends V> valueFunction) {
        return wrapped.collectKeysAndValues(iterable, keyFunction, valueFunction);
    }


    public V removeKey(String key) {
        return wrapped.removeKey(key);
    }


    public MutableMap<String, V> newEmpty() {
        return wrapped.newEmpty();
    }


    public MutableMap<String, V> clone() {
        return clone();
    }

    public V put(String key, V value) {
        return wrapped.put(key, value);
    }



    @Override
    public boolean remove(Object key) {
        return wrapped.remove(key)!=null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return values().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        for (var e : c) {
            add(e);
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }


    public void putAll(Map<? extends String, ? extends V> m) {
        wrapped.putAll(m);
    }


    public void clear() {
        wrapped.clear();
    }


    public Set<String> keySet() {
        return wrapped.keySet();
    }


    public Collection<V> values() {
        return wrapped.values();
    }


    public Set<Map.Entry<String, V>> entrySet() {
        return wrapped.entrySet();
    }


    public V get(Object key) {
        return wrapped.get(key);
    }


    public boolean containsKey(Object key) {
        return wrapped.containsKey(key);
    }


    public boolean containsValue(Object value) {
        return wrapped.containsValue(value);
    }


    public void forEachKeyValue(Procedure2<? super String, ? super V> procedure) {
        wrapped.forEachKeyValue(procedure);
    }


    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return wrapped.containsValue(o);
    }

    @Override
    public Iterator<V> iterator() {
        return wrapped.values().iterator();
    }

    @Override
    public Object[] toArray() {
        return wrapped.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return wrapped.values().toArray(a);
    }

    @Override
    public boolean add(V v) {
        put(v.getKey(), v);
        return true;
    }
}
