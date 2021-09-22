package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFuncMetadata;
import org.hpcclab.msc.object.entity.function.MscFunction;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class MscFuncRepository implements ReactivePanacheMongoRepositoryBase<MscFunction, String> {

  public Uni<MscFunction> findByName(String name) {
    return find("_id", name)
      .firstResult();
  }

  public Uni<Map<String, MscFunction>> listByMeta(List<MscFuncMetadata> funcMetadataList) {
    var names = funcMetadataList
      .stream().map(MscFuncMetadata::getName)
      .collect(Collectors.toList());
    return find("_id in ?1", names)
      .list()
      .map(l ->
        l.stream().collect(Collectors.toMap(MscFunction::getName, Function.identity()))
      );
  }

  public Uni<List<MscFunction>> listByNames(Collection<String> Names) {
    return find("_id in ?1", Names).list();
  }
}
