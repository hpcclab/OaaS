package org.hpcclab.oaas.repository;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.cls.ReferenceSpecification;
import org.hpcclab.oaas.model.cls.ResolvedMember;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger( ClassResolver.class );

  public OaasClass resolve(OaasClass base, List<OaasClass> parentClasses) {
    var resolved = base.copy();
    if (parentClasses.isEmpty()) {
      ResolvedMember resolvedMember = new ResolvedMember(
        base.getFunctions()
          .stream()
          .collect(Collectors.toMap(FunctionBinding::getName, Function.identity())),
        base.getStateSpec()
          .getKeySpecs()
          .stream()
          .collect(Collectors.toMap(KeySpecification::getName, Function.identity())),
        base.getRefSpec()
          .stream()
          .collect(Collectors.toMap(ReferenceSpecification::getName, Function.identity())),
        Set.of(),
        true
      );
      resolved.setResolved(resolvedMember);
      return resolved;
    }

    MutableMap<String, FunctionBinding> functions = Maps.mutable.empty();
    MutableMap<String, KeySpecification> keySpecs = Maps.mutable.empty();
    MutableMap<String, ReferenceSpecification> refSpecs = Maps.mutable.empty();
    Set<String> identities = Sets.mutable.empty();
    for (var parent : parentClasses) {
      var r = parent.getResolved();
      if (r.getFunctions()!=null)
        functions.putAll(r.getFunctions());
      if (r.getKeySpecs()!=null)
        keySpecs.putAll(r.getKeySpecs());
      if (r.getRefSpecs()!=null)
        refSpecs.putAll(r.getRefSpecs());
      if (r.getIdentities()!=null)
        identities.addAll(r.getIdentities());
    }
    if (base.getFunctions()!=null)
      base.getFunctions()
        .forEach(fb -> functions.put(fb.getName(), fb));
    if (base.getStateSpec().getKeySpecs()!=null)
      base.getStateSpec().getKeySpecs()
        .forEach(ks -> keySpecs.put(ks.getName(), ks));
    if (base.getRefSpec()!=null)
      base.getRefSpec()
        .forEach(rs -> refSpecs.put(rs.getName(), rs));
    if (base.getParents()!=null || !base.getParents().isEmpty()) {
      identities.addAll(base.getParents());
    }
    ResolvedMember resolvedMember = new ResolvedMember(
      functions,
      keySpecs,
      refSpecs,
      identities,
      true
    );
    return resolved.setResolved(resolvedMember);
  }
}
