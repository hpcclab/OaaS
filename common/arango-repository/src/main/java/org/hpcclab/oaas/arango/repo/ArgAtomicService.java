package org.hpcclab.oaas.arango.repo;

import com.arangodb.model.DocumentUpdateOptions;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.hpcclab.oaas.arango.ArgDataAccessException;
import org.hpcclab.oaas.repository.AtomicOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.hpcclab.oaas.arango.MutinyUtils.createUni;
import static org.hpcclab.oaas.arango.repo.AbstractArgRepository.replaceOptions;

public class ArgAtomicService<V> implements AtomicOperationService<String, V> {
  private static final Logger logger = LoggerFactory.getLogger(ArgAtomicService.class);

  AbstractArgRepository<V> repository;

  public ArgAtomicService(AbstractArgRepository<V> repository) {
    this.repository = repository;
  }

  @Override
  public Uni<V> persistWithRevAsync(V v) {
    String key = repository.extractKey(v);
    if (logger.isDebugEnabled())
      logger.debug("persistWithPreconditionAsync[{}] {}", repository.getCollection().name(), key);
    return createUni(() -> repository.getAsyncCollection()
      .replaceDocument(key, replaceOptions()))
      .replaceWith(v);
  }

  @Override
  public Uni<Void> persistWithRevAsync(Collection<V> collection) {
    if (logger.isDebugEnabled())
      logger.debug("persistWithPreconditionAsync(col)[{}] {}",
        repository.getCollection().name(), collection.size());

    return createUni(() -> repository.getAsyncCollection()
      .updateDocuments(collection, new DocumentUpdateOptions()
        .ignoreRevs(false)))
      .invoke(Unchecked.consumer(entities -> {
        if (!entities.getErrors().isEmpty())
          throw new ArgDataAccessException(entities.getErrors());
      }))
      .replaceWithVoid();
  }

}
