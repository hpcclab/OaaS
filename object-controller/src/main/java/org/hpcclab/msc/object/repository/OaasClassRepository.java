package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasClassDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OaasClassRepository implements PanacheRepositoryBase<OaasClass, String> {

  @Inject
  OaasMapper oaasMapper;

  public Uni<OaasClass> findByName(String name) {
    return find("name", name)
      .firstResult();
  }

  public Uni<List<OaasClass>> listByNames(Collection<String> Names) {
    return find("name in ?1", Names).list();
  }

  public Uni<OaasClass> save(OaasClassDto classDto) {
    return persist(oaasMapper.toClass(classDto));
  }

  public Uni<OaasClass> getDeep2(String name) {
    return getSession().flatMap(session -> {
      var clsGraph = session.getEntityGraph(OaasClass.class, "oaas.class.deep");
      return session.find(clsGraph, name);
    });
  }

  public Uni<OaasClass> getDeep(String name) {
    return find("""
      select c
      from OaasClass c
      left join fetch c.functions as fb
      join fetch fb.function
      where c.name = ?1
      """, name).singleResult();
  }

}
