package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFuncMetadata;
import org.hpcclab.msc.object.entity.MscFunction;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class MscFuncRepository implements ReactivePanacheMongoRepository<MscFunction> {

  public Uni<MscFunction> findByName(String name) {
    return find("name", name)
      .firstResult();
  }

  public Uni<Map<String, MscFunction>> listByMeta(List<MscFuncMetadata> funcMetadataList) {
    var names = funcMetadataList
      .stream().map(MscFuncMetadata::getName)
      .toList();
    return find("name in ?1", names)
      .list()
      .map(l ->
        l.stream().collect(Collectors.toMap(MscFunction::getName, Function.identity()))
      );
  }
}
