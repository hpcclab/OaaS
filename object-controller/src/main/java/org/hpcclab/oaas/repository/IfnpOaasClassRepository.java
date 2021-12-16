package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.proto.OaasClassPb;
import org.hpcclab.oaas.model.proto.OaasFunctionBindingPb;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class IfnpOaasClassRepository extends AbstractIfnpRepository<String, OaasClassPb>{

  private static final Logger LOGGER = LoggerFactory.getLogger( IfnpOaasClassRepository.class );
  private static final String NAME = OaasClassPb.class.getName();

  @Inject
  @Remote("OaasClass")
  RemoteCache<String, OaasClassPb> cache;


  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public RemoteCache<String, OaasClassPb> getRemoteCache() {
    return remoteCache;
  }

  @Override
  public String getEntityName() {
    return NAME;
  }


  public Uni<OaasClassPb> getDeep(String name) {
    //TODO
    return null;
  }

  public Uni<OaasClassPb> persist(OaasClassPb cls) {
    return this.put(cls.getName(), cls);
  }

  public Uni<Void> persist(Collection<OaasClassPb> classList) {
    return this.putAll(classList.stream()
      .collect(Collectors.toMap(OaasClassPb::getName, Function.identity()))
    );
  }


  public Optional<OaasFunctionBindingPb> findFunction(String clsName, String funcName) {
    var cls = get(clsName);
    return cls.getFunctions()
      .stream()
      .filter(fb -> fb.getFunction().equals(funcName))
      .findFirst();
  }
}
