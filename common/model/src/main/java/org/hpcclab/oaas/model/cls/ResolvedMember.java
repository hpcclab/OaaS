package org.hpcclab.oaas.model.cls;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.state.KeySpecification;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ResolvedMember implements Copyable<ResolvedMember> {
  Map<String, FunctionBinding> functionBindings;
  Map<String, KeySpecification> keySpecs;
  Map<String, ReferenceSpecification> refSpecs;

  Set<String> identities;

  public ResolvedMember() {
  }

  public ResolvedMember(Map<String, FunctionBinding> functionBindings,
                        Map<String, KeySpecification> keySpecs,
                        Map<String, ReferenceSpecification> refSpecs,
                        Set<String> identities) {
    this.functionBindings = functionBindings;
    this.keySpecs = keySpecs;
    this.refSpecs = refSpecs;
    this.identities = identities;
  }

  @Override
  public ResolvedMember copy() {
    return new ResolvedMember(
      functionBindings==null ? null:Map.copyOf(functionBindings),
      keySpecs==null ? null:Map.copyOf(keySpecs),
      refSpecs==null ? null:Map.copyOf(refSpecs),
      identities==null? null : Set.copyOf(identities)
    );
  }
}
