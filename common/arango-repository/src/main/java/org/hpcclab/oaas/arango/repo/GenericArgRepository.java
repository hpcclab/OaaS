package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;

import java.util.function.Function;

public class GenericArgRepository<V> extends AbstractArgRepository<V>{
  Class<V> valueCls;
  Function<V, String> keyExtractor;
  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;

  public GenericArgRepository(Class<V> valueCls, Function<V, String> keyExtractor, ArangoCollection collection, ArangoCollectionAsync collectionAsync) {
    this.valueCls = valueCls;
    this.keyExtractor = keyExtractor;
    this.collection = collection;
    this.collectionAsync = collectionAsync;
  }

  @Override
  public ArangoCollection getCollection() {
    return collection;
  }

  @Override
  public ArangoCollectionAsync getAsyncCollection() {
    return collectionAsync;
  }

  @Override
  public Class<V> getValueCls() {
    return valueCls;
  }

  @Override
  public String extractKey(V v) {
    return keyExtractor.apply(v);
  }


}
