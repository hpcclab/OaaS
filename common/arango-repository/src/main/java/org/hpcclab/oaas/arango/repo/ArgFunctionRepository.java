package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.FunctionRepository;

public class ArgFunctionRepository extends AbstractArgRepository<OFunction>
  implements FunctionRepository {

  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;


  public ArgFunctionRepository(ArangoCollection collection,
                               ArangoCollectionAsync collectionAsync) {
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
  public Class<OFunction> getValueCls() {
    return OFunction.class;
  }

  @Override
  public String extractKey(OFunction oaasFunction) {
    return oaasFunction.getKey();
  }

}
