package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class OaasFuncRepository implements PanacheRepositoryBase<OaasFunction, String> {

  public Uni<OaasFunction> findByName(String name) {
    return find("_id", name)
      .firstResult();
  }

  public Uni<List<OaasFunction>> listByNames(Collection<String> Names) {
    return find("_id in ?1", Names).list();
  }
}
