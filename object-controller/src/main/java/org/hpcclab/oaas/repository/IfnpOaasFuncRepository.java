package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;
import org.infinispan.client.hotrod.RemoteCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class IfnpOaasFuncRepository extends AbstractIfnpRepository<String, OaasFunctionPb> {

  private static final String NAME = OaasFunctionPb.class.getName();
  @Inject
  OaasMapper oaasMapper;

  @Inject
  @Remote("OaasFunction")
  RemoteCache<String, OaasFunctionPb> cache;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  public Uni<OaasFunctionPb> persist(OaasFunctionPb fn) {
    return this.putAsync(fn.getName(), fn);
  }

  public Uni<Void> persist(Collection<OaasFunctionPb> list) {
    return this.putAllAsync(list.stream()
      .collect(Collectors.toMap(OaasFunctionPb::getName, Function.identity()))
    );
  }
}
