package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.repository.mapper.ModelMapper;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.infinispan.client.hotrod.RemoteCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class OaasFuncRepository extends AbstractIfnpRepository<String, OaasFunction> {

  private static final String NAME = OaasFunction.class.getName();
  @Inject
  ModelMapper oaasMapper;

  @Inject
  @Remote("OaasFunction")
  RemoteCache<String, OaasFunction> cache;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  public Uni<OaasFunction> persist(OaasFunction fn) {
    return this.putAsync(fn.getName(), fn);
  }

  public Uni<Void> persist(Collection<OaasFunction> list) {
    list.forEach(OaasFunction::validate);
    return this.putAllAsync(list.stream()
      .collect(Collectors.toMap(OaasFunction::getName, Function.identity()))
    );
  }
}
