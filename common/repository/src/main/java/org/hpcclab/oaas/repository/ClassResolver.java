package org.hpcclab.oaas.repository;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.cls.ReferenceSpecification;
import org.hpcclab.oaas.model.cls.ResolvedMember;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.state.KeySpecification;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClassResolver {
  public OaasClass merge(OaasClass base, List<OaasClass> parentClasses) {
    var resolved = base.copy();
    if (parentClasses==null || parentClasses.isEmpty()) {
      ResolvedMember resolvedMember = new ResolvedMember(
        base.getFunctions()
          .stream()
          .collect(Collectors.toMap(FunctionBinding::getName, Function.identity())),
        base.getStateSpec().getKeySpecs()
          .stream()
          .collect(Collectors.toMap(KeySpecification::getName, Function.identity())),
        base.getRefSpec()
          .stream()
          .collect(Collectors.toMap(ReferenceSpecification::getName, Function.identity()))
      );
      return resolved.setResolvedMember(resolvedMember);
    }

    MutableMap<String, FunctionBinding> functionBindings = Maps.mutable.empty();
    MutableMap<String, KeySpecification> keySpecs = Maps.mutable.empty();
    MutableMap<String, ReferenceSpecification> refSpecs = Maps.mutable.empty();
    for (var parent : parentClasses) {
      var r = parent.getResolvedMember();
      functionBindings.putAll(r.getFunctionBindings());
      keySpecs.putAll(r.getKeySpecs());
      refSpecs.putAll(r.getRefSpecs());
    }
    base.getFunctions()
      .forEach(fb -> functionBindings.put(fb.getName(), fb));
    base.getStateSpec().getKeySpecs()
      .forEach(ks -> keySpecs.put(ks.getName(), ks));
    base.getRefSpec()
      .forEach(rs -> refSpecs.put(rs.getName(), rs));
    ResolvedMember resolvedMember = new ResolvedMember(
      functionBindings,
      keySpecs,
      refSpecs
    );
    return resolved.setResolvedMember(resolvedMember);
  }


}
