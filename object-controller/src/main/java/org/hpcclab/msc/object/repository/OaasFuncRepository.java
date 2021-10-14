package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasFunctionDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class OaasFuncRepository implements PanacheRepositoryBase<OaasFunction, String> {

  @Inject
  OaasMapper oaasMapper;

  public Uni<OaasFunction> findByName(String name) {
    return findById(name);
  }

  public Uni<List<OaasFunction>> listByNames(Collection<String> names) {
    return find("name in ?1", names).list();
  }

  public Uni<OaasFunction> save(OaasFunctionDto functionDto) {
    return persist(oaasMapper.toFunc(functionDto));
  }
}
