package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.MscFunction;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class MscFuncRepository implements ReactivePanacheMongoRepositoryBase<MscFunction, String> {

  public Uni<MscFunction> findByName(String name) {
    return find("_id", name)
      .firstResult();
  }

  public Uni<List<MscFunction>> listByNames(Collection<String> Names) {
    return find("_id in ?1", Names).list();
  }
}
