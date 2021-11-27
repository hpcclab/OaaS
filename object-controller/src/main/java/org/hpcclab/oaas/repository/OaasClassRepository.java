package org.hpcclab.oaas.repository;

import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.cls.OaasClassDto;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class OaasClassRepository implements PanacheRepositoryBase<OaasClass, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger( OaasClassRepository.class );

  @Inject
  OaasMapper oaasMapper;
  @Inject
  Mutiny.SessionFactory sf;


  public Uni<OaasClass> findByName(String name) {
    return findById(name);
  }

  public Uni<List<OaasClass>> listByNames(Collection<String> Names) {
    return find("name in ?1", Names).list();
  }

  public Uni<OaasClass> getDeep(String name) {
    return getSession().flatMap(session -> {
      var clsGraph = session.getEntityGraph(OaasClass.class, "oaas.class.deep");
      return session.find(clsGraph, name);
    });
  }
//
//  public Uni<OaasClass> getDeep(String name) {
//    return find("""
//      select c
//      from OaasClass c
//      left join fetch c.functions
//      where c.name = ?1
//      """, name).singleResult();
//  }

  @CacheResult(cacheName = "loadCls")
  public Uni<OaasClass> loadClsThrowOnNull(String name) {
    return sf.withStatelessSession(ss -> {
      var eg = ss.getEntityGraph(OaasClass.class,
        "oaas.class.find");
      return ss.get(eg, name).onItem().ifNull().failWith(() -> NoStackException.INSTANCE);
    });
  }

  public Uni<OaasClass> loadCls(String name) {
    return loadClsThrowOnNull(name)
      .onFailure(NoStackException.class).recoverWithNull();
  }
}
