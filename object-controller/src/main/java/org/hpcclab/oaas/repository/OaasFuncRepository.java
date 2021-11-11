package org.hpcclab.oaas.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.function.OaasFunctionDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class OaasFuncRepository implements PanacheRepositoryBase<OaasFunction, String> {

  @Inject
  OaasMapper oaasMapper;
  @Inject
  Mutiny.SessionFactory sessionFactory;

  public Uni<OaasFunction> findByName(String name) {
    return findById(name);
  }

  public Uni<List<OaasFunction>> listByNames(Collection<String> names) {
    return find("name in ?1", names).list();
  }

  public Uni<OaasFunction> save(OaasFunctionDto functionDto) {
    return persist(oaasMapper.toFunc(functionDto));
  }

  public Uni<OaasFunction> findWithClass(String name) {
    return find("""
      select f from OaasFunction f
      left join fetch f.outputCls
      where f.name = ?1
      """, name).firstResult();
  }
}
