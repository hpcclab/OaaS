package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasClassDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

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
}
